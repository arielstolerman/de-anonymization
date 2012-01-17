package reid_v01;


import java.io.*;
//import java.lang.management.*;
import java.text.*;
import java.util.*;


/**
 * Main class for generating feature values on a graph. Can be used for 
 * Identity Resolution. 
 * 
 * Returns the feature file and a file with the names of the features.
 * 
 * @author hendersk
 *
 */
public class GenerateFeatures {
	
	protected static final int MAX_ITERATIONS = 100;
	protected static final double TOLERANCE = 0.01;

	protected static Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets = 
		new HashMap<String, Map<Double, Set<AttributedNode>>>();
	protected static Map<String, Double> maxBins = 
		new HashMap<String, Double>();
	protected static Map<String, Map<String, Boolean>> memoizedMatches = 
		new HashMap<String, Map<String, Boolean>>();
	
	protected static List<String> featureNames = new ArrayList<String>();
	protected static Map<String, List<Double>> featureValues = 
		new HashMap<String, List<Double>>();
	
	protected static void computeAttributes(String graphFile, 
			int maxDist, double binSize) throws IOException{
		String curLine, uid, vid;
		Double weight;
		int numIters = 0;
		long weighted = 0, unweighted = 0;

		AttributedGraph graph = new AttributedGraph();


		BufferedReader reader = new BufferedReader(new FileReader(graphFile));

		graph.buildNodeIndex("nodeID");
		while ((curLine = reader.readLine()) != null) {
			String[] fields = curLine.split(",");
			uid = new String(fields[0]);
			vid = new String(fields[1]);
			weight = new Double(fields[2]);
			unweighted++;
			weighted += weight;

			AttributedNode srcNode = graph.getNode("nodeID", uid);
			if (srcNode == null) {
				srcNode = graph.addNode();
				srcNode.setAttr("nodeID", uid);
				graph.updateIndex(srcNode);
			}

			AttributedNode dstNode = graph.getNode("nodeID", vid);
			if (dstNode == null) {
				dstNode = graph.addNode();
				dstNode.setAttr("nodeID", vid);
				graph.updateIndex(dstNode);
			}

			Map<String,Object> attrs = new HashMap<String,Object>();
			attrs.put("wgt", weight);

			graph.addLink(srcNode, dstNode, attrs);
		}


		int j = 0;
		System.out.print(TimeUtils.dateAsString(System.currentTimeMillis())
				+ ": Iteration " + (j++));

		graph.buildNodeIndex("nodeID");

		List<String> allReps = new ArrayList<String>();

		String[] primitives = RankedRoleFinder.calculateAttrs(graph, null, null);

		/*
		 * Compute vertical bins for *all* features. Replace feature values with bin values
		 * and remove any features that offer no discriminatory power.
		 */
		Set<String> constants = new HashSet<String>();
		for(String attr : primitives) {
			sortedAttrSets.put(attr, new HashMap<Double, Set<AttributedNode>>());
			Map<Double, Set<AttributedNode>> sortedAttrSet = sortedAttrSets.get(attr);
			maxBins.put(attr, 0.0);

			
			int sum = 0;
			String ranks = RankedRoleFinder.verticalBin(graph, attr, binSize);
			for (AttributedNode n1 : graph.getNodes()) {
				double rank = (Double)n1.getAttr(ranks);
				n1.setAttr(attr, rank);
				n1.attrs.remove(ranks);
				
				if(rank > maxDist && rank-maxDist > TOLERANCE) {
					if (!sortedAttrSet.containsKey(rank)) {
						sortedAttrSet.put(rank, new HashSet<AttributedNode>());
					}
				
					sortedAttrSet.get(rank).add(n1);
				}
				
				if(rank > maxBins.get(attr))
					maxBins.put(attr, rank);
				
				sum += rank;
			}
			if(sum == 0) {
				sortedAttrSets.remove(attr);
				maxBins.remove(attr);
				for (AttributedNode n1 : graph.getNodes()) {
					n1.attrs.remove(attr);
					constants.add(attr);
				}
			}
		}

//		for (AttributedNode n1 : graph.getNodes()) {
//			System.out.println(n1.attrs);
//		}

		

		Set<String> candidates = new HashSet<String>();
		for (String s : primitives) {
			if (!constants.contains(s)) {
				candidates.add(s);
			}
		}
		
		Set<String> reps = RankedRoleFinder.calculateReps(graph, maxDist, candidates, maxBins,
				sortedAttrSets, memoizedMatches);
		
		
		for(String attr : candidates) {
			if(allReps.contains(attr)) {
				reps.add(attr);
			}
			else if (!reps.contains(attr)){
				for(AttributedNode n : graph.getNodes()) {
					n.attrs.remove(attr);
				}
			}
		}

		String[] attrs = new String[reps.size()];
		int i = 0;

		for(String rep : reps) {
			allReps.add(rep);
			attrs[i++] = RankedRoleFinder.verticalBin(graph, rep, binSize);
		}

		numIters = 1;

		System.out.println(" " + allReps.size() + " features");
		
		while(attrs.length > 0 && numIters++ < MAX_ITERATIONS) {
			maxDist += 1;
			String[] features = RankedRoleFinder.calculateAttrs(graph, null, attrs);
			
			System.out.print(TimeUtils.dateAsString(System.currentTimeMillis())
					+ ": Iteration " + (j++));

			/*
			 * Compute vertical bins for *all* features. Replace feature values with bin values
			 * and remove any features that offer no discriminatory power.
			 */
			constants = new HashSet<String>();
			for(String attr : features) {
				sortedAttrSets.put(attr, new HashMap<Double, Set<AttributedNode>>());
				Map<Double, Set<AttributedNode>> sortedAttrSet = sortedAttrSets.get(attr);
				maxBins.put(attr, 0.0);
				
				
				int sum = 0;
				String ranks = RankedRoleFinder.verticalBin(graph, attr, binSize);
				for (AttributedNode n1 : graph.getNodes()) {
					double rank = (Double)n1.getAttr(ranks);
					n1.setAttr(attr, rank);
					n1.attrs.remove(ranks);
					
					if(rank > maxDist && rank-maxDist > TOLERANCE) {
						if (!sortedAttrSet.containsKey(rank)) {
							sortedAttrSet.put(rank, new HashSet<AttributedNode>());
						}
					
						sortedAttrSet.get(rank).add(n1);
					}
					
					if(rank > maxBins.get(attr))
						maxBins.put(attr, rank);
					
					sum += rank;
				}
				if(sum == 0) {
					sortedAttrSets.remove(attr);
					maxBins.remove(attr);
					for (AttributedNode n1 : graph.getNodes()) {
						n1.attrs.remove(attr);
						constants.add(attr);
					}
				}
			}

//			for (AttributedNode n1 : graph.getNodes()) {
//				System.out.println(n1.attrs);
//			}

			
			
			

			candidates = new HashSet<String>();
			for (String s : features) {
				if (!constants.contains(s)) {
					candidates.add(s);
				}
			}
			for(String s : allReps) candidates.add(s);


			reps = RankedRoleFinder.calculateReps(graph, maxDist, candidates, maxBins,
					sortedAttrSets, memoizedMatches);

			for(String attr : candidates) {
				if(allReps.contains(attr)) {
					reps.add(attr);
				}
				else if(!reps.contains(attr)){
					for(AttributedNode n : graph.getNodes()) {
						n.attrs.remove(attr);
					}
				}
			}

			attrs = new String[reps.size() - allReps.size()];
			i = 0;

			for (String rep : reps) {
				if(allReps.contains(rep)) continue;
				allReps.add(rep);
				attrs[i++] = RankedRoleFinder.verticalBin(graph, rep, binSize);
			}
			System.out.println(" " + allReps.size() + " features");

		}

		for(String rep : allReps) {
			featureNames.add(rep);
		}
		
		for(AttributedNode node : graph.getNodes()) {
			String nodeID = (String)node.getAttr("nodeID");
			featureValues.put(nodeID, new ArrayList<Double>());
			for(String comp : featureNames) 
				featureValues.get(nodeID).add((Double)node.getAttr(comp));			
		}
		sortedAttrSets.clear();
		graph = null;
		System.gc();


		System.out.println("Graph: " + graphFile);
		System.out.println("Nodes: " + featureValues.size());
		System.out.println("Edges: " + unweighted);
		System.out.println("Edge Weight: " + weighted);
	}
	
		
	protected static void writeFeatures(
			Map<String, List<Double>> featureValues,
			String fileName) throws IOException {

		PrintStream out = new PrintStream(fileName);
		for(String s : featureValues.keySet()) {
			out.print(s + ",");
			List<Double> vals = featureValues.get(s);
			for(int i = 0; i < vals.size()-1; i++)
				out.print(vals.get(i) + ",");
			out.println(vals.get(vals.size()-1));
		}
	}
	
	
	/**
	 * Entry point for feature generation. Takes a graph as input along with
	 * a couple parameters and outputs feature values and names to files.
	 * 
	 * 
	 * All graphs in 3-column .csv format source,target,weight 
	 * (String,String,Double)
	 * 
	 * @param args = targetFile referenceFile maxDist binSize baseOutFile
	 * 	graphFile: filename of graph edgelist
	 * 	maxDist: usually 0 -- this is the initial lattice error threshold
	 * 	binSize: usually 0.5 -- this is the fraction in each bin
	 * 	baseOutFile: base for output filenames.
	 * 
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException{
		String graphFile = null;
		int maxDist = -1;
		double binSize = -1;
		String featFile = null, outFile = null;
		String baseOut = null;
		
		/*
		try {
			graphFile = args[0];
			maxDist = new Integer(args[1]).intValue();
			binSize = new Double(args[2]).doubleValue();
			baseOut = args[3];
		}
		catch(Exception e) {
			System.err.println("Usage: java IDResGen.jar targetGraphFile " + 
			"referenceGraphFile maxDist binSize outputFileBase");
			return;
		}
		*/
		
		//TODO hardcoded
		graphFile = "./data/allawsv-anon.2011-09.txt";
		maxDist = 3;
		binSize = 0.5;
		baseOut = "./data/att";
		

		featFile = baseOut + "-featureNames.csv";
		outFile = baseOut + "-featureValues.csv";
		
		System.out.println("Lattice Threshold = " + maxDist);
		System.out.println("Bin Size = " + binSize);
		System.out.println();
		
		
		computeAttributes(graphFile, maxDist, binSize);
		System.out.println();
		
		
		int depth = 1;
		for(String s : featureNames) {
			int d = s.split("-").length;
			if (d > depth) depth = d;
		}

		System.out.println(featureNames.size() + " features");
		System.out.println(depth + " iterations");
		System.out.println();

		System.out.println("Feature Names: writing " + featFile);
		
		PrintStream out = new PrintStream(featFile);
		for(int i = 0; i < featureNames.size()-1; i++) 
			out.print(featureNames.get(i) + ",");
		out.println(featureNames.get(featureNames.size()-1));
		
		System.out.println("Feature Values: writing " + outFile);
		writeFeatures(featureValues, outFile);
		System.out.println();
		
		
	}
	

	
//	static long usedMemory() {
//		return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
//	}
//	static String usedMemStr() {
//		long bytes = usedMemory();
//		if(bytes < 1000) return ""+bytes + " B";
//		if(bytes < 1000000) return ""+bytes/1000 + " KB";
//		if(bytes < 1000000000) return ""+bytes/1000000 + " MB";
//		return ""+ bytes/1000000000 + " GB";
//	}
	
}
