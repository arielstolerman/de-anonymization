package reid_v01;


import java.io.*;
//import java.lang.management.*;
import java.text.*;
import java.util.*;


/**
 * 
 * @author hendersk
 *
 */
public class CalculateFeatures {
	
	protected static final int MAX_ITERATIONS = 100;
	protected static final double TOLERANCE = 0.01;

	
	protected static List<String> featureNames = new ArrayList<String>();
	protected static Map<String, List<Double>> featureValues = 
		new HashMap<String, List<Double>>();
	
	
	protected static void calculateAttributes(String graphFile, 
			double binSize) throws IOException{
		String curLine, uid, vid;
		Double weight;
		int numIters = 0;
		long weighted=0, unweighted=0;
		
		
		
		Set<String> compNameSet = new HashSet<String>(featureNames);
		
		Set<String> validPostfix = new HashSet<String>();
		List<String> candidatePostfix = new ArrayList<String>();
		List<String> postfix = new ArrayList<String>();
		
		for(String s : featureNames) {
			if(s.indexOf('-') > -1)
				validPostfix.add("wgt"+s.substring(s.indexOf('-')));
		}
		
		AttributedGraph graph = new AttributedGraph();


		BufferedReader reader = new BufferedReader(new FileReader(graphFile));

		graph.buildNodeIndex("nodeID");
		while ((curLine = reader.readLine()) != null) {
			String[] fields = curLine.split(",");
			uid = new String(fields[0]);
			vid = new String(fields[1]);
			weight = new Double(fields[2]);

			unweighted++;
			weighted+=weight;
			
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



		graph.buildNodeIndex("nodeID");
		
		int j = 0;
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis())
				+ ": Iteration " + (j++));

		List<String> allReps = new ArrayList<String>();

		String[] primitives = RankedRoleFinder.calculateAttrs(graph, null, null);
		
		/*
		 * Compute vertical bins for *all* features. Replace feature values with bin values
		 * and remove any features that were not selected by the target graph.
		 */
		for(String attr : primitives) {
			int sum = 0;
			String ranks = RankedRoleFinder.verticalBin(graph, attr, binSize);
			for (AttributedNode n1 : graph.getNodes()) {
				double rank = (Double)n1.getAttr(ranks);
				n1.setAttr(attr, rank);
				n1.attrs.remove(ranks);
				
				
				sum += rank;
			}
			
		}


		
//		for (AttributedNode n1 : graph.getNodes()) {
//			System.out.println(n1.attrs);
//		}

		

		Set<String> candidates = new HashSet<String>();
		for (String s : primitives) {
				candidates.add(s);
		}
		
		
		Set<String> reps = new HashSet<String>();
		for(String s : candidates)
			if(featureNames.contains(s))
				reps.add(s);
		
		for(String attr : candidates) {
			if (!reps.contains(attr)){
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

		
		
		while(attrs.length > 0 && numIters++ < MAX_ITERATIONS) {
			
			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis())
					+ ": Iteration " + (j++));
			
			String[] features = RankedRoleFinder.calculateAttrs(graph, null, attrs);
			
			
			/*
			 * Compute vertical bins for *all* features. Replace feature values with bin values
			 * and remove any features that offer no discriminatory power.
			 */
			for(String attr : features) {
				int sum = 0;
				String ranks = RankedRoleFinder.verticalBin(graph, attr, binSize);
				for (AttributedNode n1 : graph.getNodes()) {
					double rank = (Double)n1.getAttr(ranks);
					n1.setAttr(attr, rank);
					n1.attrs.remove(ranks);
					sum += rank;
				}
			}


			
			

			candidates = new HashSet<String>();
			for (String s : features) {
				candidates.add(s);
			}
			for(String s : allReps) candidates.add(s);

			reps = new HashSet<String>();
			for(String s : candidates) {
				if(compNameSet.contains(s))
					reps.add(s);
			}

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


			
			candidatePostfix.clear();
			for (String rep : reps) {
				if(allReps.contains(rep)) continue;
				allReps.add(rep);
				candidatePostfix.add(RankedRoleFinder.verticalBin(graph, rep, binSize));
			}
			
			postfix.clear();
			for(String post : candidatePostfix) {
				if(validPostfix.contains(post)) 
					postfix.add(post);
			}
			
			attrs = new String[postfix.size()];
			i = 0;

			for (String post : postfix) {
				attrs[i++] = post;
			}

		}


		
		for(AttributedNode node : graph.getNodes()) {
			String nodeID = (String)node.getAttr("nodeID");			
			featureValues.put(nodeID, new ArrayList<Double>());
			for(String comp : featureNames) 
				featureValues.get(nodeID).add((Double)node.getAttr(comp));			
		}
		


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
	 * Entry point for fixed feature calculation. Here we know the features
	 * that we want, so pruning is done simply by removing any unwanted
	 * features by name. Feature values are written to a file.
	 * 
	 * All graphs in 3-column .csv format source,target,weight 
	 * (String,String,Double)
	 * 
	 * @param args = 
	 * 	graphFileName : name of u,v,w .csv file containing edgelist
	 *  featureFileName : name of .csv file with one line containing feature
	 *  	names to keep
	 *  binSize : size of vertical logarithmic bins (usually 0.5)
	 *  baseOutputFileName : base filename for feature values.
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException{
		String graphFile = null;
		double binSize = -1;
		String featFile = null, outFile = null;
		String baseOut = null;
		try {
			graphFile = args[0];
			featFile = args[1];
			binSize = Double.parseDouble(args[2]);
			baseOut = args[3];
		}
		catch(Exception e) {
			System.err.println("Usage: java IDResFixed.jar " + 
			"referenceGraphFile featureFile binSize outputFileBase");
			return;
		}

		outFile = baseOut + "-featureValues.csv";
		
		
		for(String rep : new BufferedReader(
				new FileReader(featFile)).readLine().split(",")) {
			featureNames.add(rep);
		}
		
		
		calculateAttributes(graphFile, binSize);
		System.out.println();
		
		int depth = 1;
		for(String s : featureNames) {
			int d = s.split("-").length;
			if (d > depth) depth = d;
		}

		System.out.println(featureNames.size() + " features");
		System.out.println(depth + " iterations");
		System.out.println();

		
		
		System.out.println("Feature Values: writing " + outFile);
		writeFeatures(featureValues, outFile);
		System.out.println();
		
		
	}
}