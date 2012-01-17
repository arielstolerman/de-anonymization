package reid;

import java.io.*;
import java.util.*;

public class EdgeGuesser {

	/********************************************************************************
	 * Instance members
	 ********************************************************************************/
	
	protected List<String> featureNames = new ArrayList<String>();
	protected Map<String, Integer> featureIndices = new HashMap<String, Integer>();
	protected Map<String, Feature> refFeatures = new HashMap<String, Feature>();
	protected Map<String, Feature> tarFeatures = new HashMap<String, Feature>();
	protected List<Feature> tarSortedFeatures = null;
	protected Map<String, HashSet<Node>> tarGraphAdjList = new HashMap<String, HashSet<Node>>();
	protected Map<String, HashSet<String>> refGraphAdjList = new HashMap<String, HashSet<String>>();

	protected String baseOut = null;

	protected enum EdgeType {
		SOURCE, DESTINATION
	}
	
	// Store information about edges
	protected class Node {
		
		public Node(String name, EdgeType type) {
			this.name = name;
			this.type = type;
		}
		
		String name;
		EdgeType type;
	}

	// Edge distance object used to store and sort edges by distance
	protected class EdgeDistance implements Comparable<EdgeDistance> {
		public EdgeDistance(String src, String dst, double distance) {
			this.src = src;
			this.dst = dst;
			this.distance = distance;
		}
		
		public String src;
		public String dst;
		public double distance;
		
		public int compareTo(EdgeDistance o) {
			if((this.distance - o.distance) < 0.0000001) return 0;
			return (int)(this.distance - o.distance);
		}
	}
	
	/********************************************************************************
	 *  Getter Methods
	 ********************************************************************************/
	
	public List<String> getFeatureNames() {
		return Collections.unmodifiableList(this.featureNames);
	}
	
	public List<Feature> getSortedTarFeatures() {
		if (this.tarSortedFeatures == null) {
			this.tarSortedFeatures = new ArrayList<Feature>(this.tarFeatures.values());
			Collections.sort(this.tarSortedFeatures);
		}
		return Collections.unmodifiableList(this.tarSortedFeatures);
	}
	
	/********************************************************************************
	 *  Constructor
	 ********************************************************************************/
	
	public EdgeGuesser(String baseIn, String baseOut, String featureType) throws IOException 
	{
		String inTarget = baseIn + "-target.txt";
		String inReference = baseIn + "-reference.txt";
		String inFeatureNames = baseIn + "-features.txt";
		String inTargetGraph = baseIn + "-target-graph.txt";
		String inReferenceGraph = baseIn + "-reference-graph.txt";

		this.baseOut = baseOut + "-" + featureType;

		this.readFeatures(featureType, inFeatureNames);
		this.readReferenceFeatures(inReference);
		this.readTargetFeatures(inTarget);
		this.readTargetGraph(inTargetGraph);
		this.readReferenceGraph(inReferenceGraph);
	}
	
	/********************************************************************************
	 *  Instance Methods
	 ********************************************************************************/
	
	protected void readFeatures(String featureType, String fname) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		String curLine = reader.readLine();
		
		// These features are local
		Set<String> locals = new HashSet<String>();
		locals.add("wn1");
		locals.add("xesu0");
		locals.add("xedu0");
		locals.add("xeu0");
		locals.add("xest0");
		locals.add("xedt0");
		locals.add("xet0");
		
		
		// Decide which columns to use based on featureType
		int index = 0;
		for (String feat : curLine.split(",")) {
			
			// Local features defined above
			if(featureType.equals("local") && 
					locals.contains(feat)) {
				this.featureNames.add(feat);
				this.featureIndices.put(feat, index);
			}
			
			// Neighborhood features have no hyphens (non-recursive)
			else if(featureType.equals("neighborhood") &&
					feat.indexOf('-') < 0) {
				this.featureNames.add(feat);
				this.featureIndices.put(feat,index);
			}
			
			// Regional features include all columns
			else if(featureType.equals("regional")){
				this.featureNames.add(feat);
				this.featureIndices.put(feat,index);
			}
			index++;
		}
		
		reader.close();
	}

	protected void readReferenceFeatures(String fname) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		String curLine = null;
		String[] split = null;
		while((curLine = reader.readLine()) != null) {
			split = curLine.split(",");
			String user = split[0];
			List<Double> features = new ArrayList<Double>();
			for(String feat : this.featureNames) {
				features.add(Double.parseDouble(split[1+this.featureIndices.get(feat)]));
			}
			this.refFeatures.put(user, new ReFeXFeature(user, features));
		}
		reader.close();
	}
	
	protected void readTargetFeatures(String fname) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		String curLine = null;
		String[] split = null;
		while((curLine = reader.readLine()) != null) {
			split = curLine.split(",");
			String user = split[0];
			// Only care about target features in reference graph
			if(!this.refFeatures.containsKey(user)) continue;
			List<Double> features = new ArrayList<Double>();
			for(String feat : this.featureNames) {
				features.add(Double.parseDouble(split[1+this.featureIndices.get(feat)]));
			}
			Feature f = new ReFeXFeature(user, features);
			this.tarFeatures.put(user, f);
		}
		reader.close();
	}
	
	protected void readTargetGraph(String fname) throws IOException {
		// Read target graph into an adjacency list for edge sampling.
		String curLine = null;
		String[] split = null;
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		while((curLine = reader.readLine()) != null) {
			split = curLine.split(",");
			String src = split[0].trim();
			String dst = split[1].trim();
			if (!this.tarGraphAdjList.containsKey(src))
				this.tarGraphAdjList.put(src, new HashSet<Node>());
			if (!this.tarGraphAdjList.containsKey(dst))
				this.tarGraphAdjList.put(dst, new HashSet<Node>());
			this.tarGraphAdjList.get(src).add(new Node(dst, EdgeType.DESTINATION));
			this.tarGraphAdjList.get(dst).add(new Node(src, EdgeType.SOURCE));
		}
		reader.close();
	}
	
	protected void readReferenceGraph(String fname) throws IOException {
		// Read reference graph into an adjacency list for edge sampling
		// Don't duplicate like we do for targetGraph
		String curLine = null;
		String[] split = null;
		BufferedReader reader = new BufferedReader(new FileReader(fname));
		while((curLine = reader.readLine()) != null) {
			split = curLine.split(",");
			String src = split[0].trim();
			String dst = split[1].trim();
			if (!this.refGraphAdjList.containsKey(src))
				this.refGraphAdjList.put(src, new HashSet<String>());
			
			this.refGraphAdjList.get(src).add(dst);
		}
		reader.close();
	}

	// Computes the square of the distance between vectors p1 and p2
	protected double euclid(Double[] p1, Double[] p2) { 
		double diff, d = 0.0;
		for (int i = 0; i < p1.length; i++) {
			diff = p1[i]-p2[i];
			d += diff*diff;
		}
		return d;
	}

	protected void writeEdgeGuessesToFile(
			String ofname, 
			String target, 
			List<EdgeDistance> edgeDistances
	) throws IOException {
		// Write guesses to file
		Writer writer = new FileWriter(ofname);
		for (EdgeDistance distance : edgeDistances) {
			writer.write(String.format("%s,%s,%f\n", distance.src, distance.dst, distance.distance));
		}
		writer.flush();
		writer.close();
		
	}
	
	public void generateGuesses(String node) throws IOException {
		// Get random sampling of edges
		Random rand = new Random();
		HashSet<Node> tarNodeEdges = this.tarGraphAdjList.get(node);
		HashSet<Node> edges = new HashSet<Node>();
		double sampleSize = 10.0;
		double threshold = sampleSize / tarNodeEdges.size();
		for (Node s : tarNodeEdges) {
			// Keep only about sampleSize elements
			if (rand.nextDouble() < threshold)
				edges.add(s);
		}
		
		// Loop over randomly selected edges in target graph
		Feature tarNode = this.tarFeatures.get(node);
		(new File(String.format("%s/%s", this.baseOut, tarNode.getName()))).mkdirs();
		for (Node edgeNode : edges) {
			Feature tarDstNode = this.tarFeatures.get(edgeNode.name);
			if (null == tarDstNode) continue;
			List<EdgeDistance> edgeDistances = new ArrayList<EdgeDistance>();
			
			// Get the output filename => baseOut/srcNode-dstNode-tarNode.csv
			String ofname = null, format = "%s/%s/%s-%s.csv";
			if (EdgeType.DESTINATION == edgeNode.type)
				ofname = String.format(format, this.baseOut, tarNode.getName(),
						tarNode.getName(), tarDstNode.getName());
			else
				ofname = String.format(format, this.baseOut, tarNode.getName(),
						tarDstNode.getName(), tarNode.getName());
			
			// Calculate distance w.r.t. target for each edge in reference graph
			for (Map.Entry<String, HashSet<String>> entry : this.refGraphAdjList.entrySet()) {
				Feature refNode = this.refFeatures.get(entry.getKey());
				if (null == refNode) continue;
				for (String nodeName : entry.getValue()) {
					Feature refDstNode = this.refFeatures.get(nodeName);
					if (null == refDstNode) continue;
					// Since direction affects distance, take it into account
					double distance = 0.0;
					if (EdgeType.SOURCE == edgeNode.type) {
						distance += tarDstNode.getDistance(refNode);
						distance += tarNode.getDistance(refDstNode);
					} else {
						distance += tarNode.getDistance(refNode);
						distance += tarDstNode.getDistance(refDstNode);
					}
					edgeDistances.add(
							new EdgeDistance(refNode.getName(), refDstNode.getName(), distance));
				}
			}
			// Sort in order of decreasing distance from target node
			Collections.sort(edgeDistances);
			this.writeEdgeGuessesToFile(ofname, tarNode.getName(), edgeDistances);
		}
		
	}
	
}
