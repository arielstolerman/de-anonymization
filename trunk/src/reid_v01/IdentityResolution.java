package reid_v01;


import java.io.*;
//import java.lang.management.*;
import java.text.*;
import java.util.*;


/**
 * Main class for the basic identity resolution class. Given a reference (training) graph
 * and a target (test) graph, it determines (on the target graph) the set of features using
 * the lattice-based feature pruning strategy. It computes these on both graphs, and writes
 * three files.
 * 
 * @author hendersk
 *
 */
public class IdentityResolution {
	
	protected static final int MAX_ITERATIONS = 100;
	protected static final double TOLERANCE = 0.01;

	protected static Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets = 
		new HashMap<String, Map<Double, Set<AttributedNode>>>();
	protected static Map<String, Double> maxBins = new HashMap<String, Double>();
	protected static Map<String, Map<String, Boolean>> memoizedMatches = 
		new HashMap<String, Map<String, Boolean>>();
	
	protected static List<String> primNames = new ArrayList<String>();
	protected static List<String> compNames = new ArrayList<String>();
	protected static Map<String, List<Double>> refPrims = new HashMap<String, List<Double>>();
	protected static Map<String, List<Double>> refComps = new HashMap<String, List<Double>>();
	protected static Map<String, List<Double>> tarPrims = new HashMap<String, List<Double>>();
	protected static Map<String, List<Double>> tarComps = new HashMap<String, List<Double>>();
	
	
	//TODO added for resetting
	public static void resetStaticVars() {
		sortedAttrSets = new HashMap<String, Map<Double, Set<AttributedNode>>>();
		maxBins = new HashMap<String, Double>();
		memoizedMatches = new HashMap<String, Map<String, Boolean>>();

		primNames = new ArrayList<String>();
		compNames = new ArrayList<String>();
		refPrims = new HashMap<String, List<Double>>();
		refComps = new HashMap<String, List<Double>>();
		tarPrims = new HashMap<String, List<Double>>();
		tarComps = new HashMap<String, List<Double>>();
	}
	
	protected static void computeTarAttrs(String graphFile, 
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
			if(rep.indexOf('-') == -1) primNames.add(rep);
			else compNames.add(rep);
		}
		
		for(AttributedNode node : graph.getNodes()) {
			String nodeID = (String)node.getAttr("nodeID");
			tarPrims.put(nodeID, new ArrayList<Double>());
			for(String prim : primNames) tarPrims.get(nodeID).add((Double)node.getAttr(prim));
			tarComps.put(nodeID, new ArrayList<Double>());
			for(String comp : compNames) tarComps.get(nodeID).add((Double)node.getAttr(comp));			
		}
		sortedAttrSets.clear();
		graph = null;
		System.gc();


		System.out.println("Target Graph: " + graphFile);
		System.out.println("Target Graph: " + tarPrims.size() + " nodes");
		System.out.println("Target Graph: " + unweighted + " edges");
		System.out.println("Target Graph: " + weighted + " total edge weight");
	}
	
	protected static void computeRefAttrs(String graphFile, 
			double binSize) throws IOException{
		String curLine, uid, vid;
		Double weight;
		int numIters = 0;
		long weighted=0, unweighted=0;
		
		
		
		Set<String> primNameSet = new HashSet<String>(primNames);
		Set<String> compNameSet = new HashSet<String>(compNames);
		
		Set<String> validPostfix = new HashSet<String>();
		List<String> candidatePostfix = new ArrayList<String>();
		List<String> postfix = new ArrayList<String>();
		
		for(String s : compNames) {
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
			if(primNameSet.contains(s))
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
			refPrims.put(nodeID, new ArrayList<Double>());
			for(String prim : primNames) 
				refPrims.get(nodeID).add((Double)node.getAttr(prim));
			
			refComps.put(nodeID, new ArrayList<Double>());
			for(String comp : compNames) 
				refComps.get(nodeID).add((Double)node.getAttr(comp));			
		}
		


		graph = null;
		System.gc();


		System.out.println("Reference Graph: " + graphFile);
		System.out.println("Reference Graph: " + refPrims.size() + " nodes");
		System.out.println("Reference Graph: " + unweighted + " edges");
		System.out.println("Reference Graph: " + weighted + " total edge weight");

		
	}
	
	
	
	
	
	protected static void writeFeatures(Map<String, List<Double>> primitives,
			Map<String, List<Double>> compounds,
			String fileName) throws IOException {

		PrintStream out = new PrintStream(fileName);
		for(String s : primitives.keySet()) {
			out.print(s + ",");
			for(double d : primitives.get(s))
				out.print(d+",");
			List<Double> comps = compounds.get(s);
			for(int i = 0; i < comps.size()-1; i++)
				out.print(comps.get(i) + ",");
			out.println(comps.get(comps.size()-1));
		}
	}
	
	// TODO for testing
	//public static String base = "./data/net-frac-0.1_lattice-threshold-0_bin-size-0.5_target-voice_ref-sms/att";
	public static String base = "./data/all_lattice-threshold-3_bin-size-0.5_voice-tar_sms-ref/att";
	
	/**
	 * Entry point for IDRes. Computes CDFs for 3 models that compare nodes in the target graph
	 * to nodes in the reference graph. Baseline assumes a uniform distribution (i.e. the rank
	 * of the "true" target node in the reference graph is equally likely to be any number in 
	 * 1...N_ref. Primitive and Compound use the RFD features, and an empirical CDF is reported.
	 * Note that all CDFs are really cumulative sums, but can be easily normalized in post.
	 * 
	 * All graphs in 3-column .csv format source,target,weight (String,String,Double)
	 * 
	 * @param args = targetFile referenceFile maxDist binSize baseOutFile
	 * 	targetFile: filename of "anonymous" graph whose nodes will be "found" in reference graph
	 * 	referenceFile: filename of "known" graph of reference nodes
	 * 	maxDist: usually 0 -- this is the initial lattice error threshold
	 * 	binSize: usually 0.5 -- this is the fraction in each bin
	 * 	baseOutFile: base for output filenames.
	 * 
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException{
		String graphFile1 = null, graphFile2 = null;
		int maxDist = -1;
		double binSize = -1;
		String featFile = null, outFile1 = null, outFile2 = null;
		String baseOut = null;
		
		
		try {
			graphFile1 = args[0];
			graphFile2 = args[1];
			maxDist = new Integer(args[2]).intValue();
			binSize = new Double(args[3]).doubleValue();
			baseOut = args[4];
		}
		catch(Exception e) {
			System.err.println("Usage: java IDResGen.jar targetGraphFile " + 
			"referenceGraphFile maxDist binSize outputFileBase");
			return;
		}
		
		
		//TODO hardcoded
		/*
		graphFile1 = base+"-target-graph.txt";
		graphFile2 = base+"-reference-graph.txt";
		maxDist = 0;
		binSize = 0.5;
		baseOut = base;
		*/
		
		featFile = baseOut + "-features.txt";
		outFile1 = baseOut + "-target.txt";
		outFile2 = baseOut + "-reference.txt";
		
		System.out.println("Lattice Threshold = " + maxDist);
		System.out.println("Bin Size = " + binSize);
		System.out.println();
		
		
		computeTarAttrs(graphFile1, maxDist, binSize);
		System.out.println();
		
		computeRefAttrs(graphFile2, binSize);
		System.out.println();
		
		int depth = 1;
		for(String s : compNames) {
			int d = s.split("-").length;
			if (d > depth) depth = d;
		}

		System.out.println(primNames.size() + " primitives");
		System.out.println(compNames.size() + " compounds");
		System.out.println(depth + " iterations");
		System.out.println();

		System.out.println("Features: writing " + featFile);
		
		PrintStream out = new PrintStream(featFile);
		for(String f : primNames) out.print(f + ",");
		for(int i = 0; i < compNames.size()-1; i++) 
			out.print(compNames.get(i) + ",");
		out.println(compNames.get(compNames.size()-1));
		
		System.out.println("Target: writing " + outFile1);
		writeFeatures(tarPrims, tarComps, outFile1);
		System.out.println("Reference: writing " + outFile2);
		writeFeatures(refPrims, refComps, outFile2);
		System.out.println();
		
		
	}
	

}
