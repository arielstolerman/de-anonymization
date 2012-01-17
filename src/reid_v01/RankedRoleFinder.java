package reid_v01;



import java.io.*;

import java.text.*;
import java.util.*;



public class RankedRoleFinder {

	private static final int MAX_ITERATIONS = 100;
	private static final double TOLERANCE = 0.01;

	private static Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets = 
		new HashMap<String, Map<Double, Set<AttributedNode>>>();
	private static Map<String, Double> maxBins = new HashMap<String, Double>();
	private static Map<String, Map<String, Boolean>> memoizedMatches = 
		new HashMap<String, Map<String, Boolean>>();
	
	// to see how much time we save over correlation
	private static double checks = 0;
	private static double corrChecks = 0;
	
	static String[] firstIteration(AttributedGraph graph, Collection<AttributedNode> nodes) {
		if(nodes == null)
			nodes = graph.getNodes();

		String[] properties = {
				"wn",
				"weu",
				"wea-wgt",
				"wem",
				"xesu",
				"xesa-wgt",
				"xesm",
				"xedu",
				"xeda-wgt",
				"xedm",
				"xeu",
				"xea-wgt",
				"xem",
		};


		String[] ret = new String[properties.length*2];
		int k = 0;
		for(String property : properties) {
			ret[k] = property.replace("a-wgt", "t") + "0";
			ret[(k++) + properties.length] = property.replace("a-wgt", "t") + "1";
		}


		EgonetGenerator egoGen = new EgonetGenerator(graph, null, null, new String[]{"wgt"});
		for(AttributedNode node : nodes) {
			Map<String, Double> counts; 

			counts = egoGen.getCounts(node.id, 0);
			for(String base : new String[]{"we", "xes", "xed", "xe"}) {
				if(counts.get(base+"u") > 0) {
					counts.put(base+"m", counts.get(base+"a-wgt")/counts.get(base+"u"));
				}
				else {
					counts.put(base+"m", 0.0);
				}
			}
			for(String property : properties) {
				node.setAttr(property.replace("a-wgt", "t")+'0', counts.get(property));
			}

			counts = egoGen.getCounts(node.id, 1);
			for(String base : new String[]{"we", "xes", "xed", "xe"}) {
				if(counts.get(base+"u") > 0) {
					counts.put(base+"m", counts.get(base+"a-wgt")/counts.get(base+"u"));
				}
				else {
					counts.put(base+"m", 0.0);
				}
			}
			for(String property : properties) {
				node.setAttr(property.replace("a-wgt", "t")+'1', counts.get(property));
			}
			
		}

		//System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " " + "Done.");

		return ret;
	}

	static String[] nextIteration(AttributedGraph graph, Collection<AttributedNode> nodes,
			String[] attrs) {
		if(nodes == null)
			nodes = graph.getNodes();

		String[] properties = {
				"xes",
				"xed",
				"xe",
				"wn",
				"wnm"
		};

		
		
		String[] ret = new String[properties.length*2*attrs.length];
		int k = 0;
		for(String attr : attrs) {
			for(String property : properties) {
				ret[k] = property + "0-" + attr.replace("wgt-", "");
				ret[(k++) + properties.length*attrs.length] = 
					property + "1-" + attr.replace("wgt-", "");
			}	
		}

		
		EgonetGenerator egoGen = new EgonetGenerator(graph, null, null, null, attrs);
		

		
		
		//EgonetGenerator egoGen = new EgonetGenerator(graph, new String[]{"type"}, 
		//	new String[]{"follows"}, null);
		//System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
		//" Computing compound features.");
		Map<String, Double> counts; 
		for(AttributedNode node : nodes) {

			counts = egoGen.getCounts(node.id, 0);
			//Set<AttributedNode> egonet = new Egonet(node, 1).getNodes();

			for(String attr : attrs) {
				counts.put("wna-"+attr, (Double)node.getAttr(attr));

				for(String base : new String[]{"xe", "xes", "xed"}) {
					if(counts.get(base+"u") > 0) {
						counts.put(base+"m-"+attr, counts.get(base+"a-"+attr)/counts.get(base+"u"));
					}
					else {
						counts.put(base+"m-"+attr, 0.0);
					}
				}
				counts.put("wnm-"+attr, counts.get("wna-"+attr) / counts.get("wn"));
			}

			for(String attr : attrs) {
				for(String property : properties) {
					if(property.endsWith("m")) {
						node.setAttr(property+"0-" + attr.replace("wgt-", ""),
								counts.get(property + "-" + attr));
					}
					else {
						node.setAttr(property + "0-" + attr.replace("wgt-", ""),
								counts.get(property + "a-" + attr));
					}
				}
			}

			counts = egoGen.getCounts(node.id, 1);

			for(String attr : attrs) {
				double count = 0;
				for(AttributedLink link : node.getLinks()) {
					AttributedNode neighbor = link.src.equals(node) ? link.dst : link.src;
					count += (Double) neighbor.getAttr(attr);
				}
				counts.put("wna-"+attr, count);


				for(String base : new String[]{"xe", "xes", "xed"}) {
					if(counts.get(base+"u") > 0) {
						counts.put(base+"m-"+attr, counts.get(base+"a-"+attr)/counts.get(base+"u"));
					}
					else {
						counts.put(base+"m-"+attr, 0.0);
					}
				}
				counts.put("wnm-"+attr, counts.get("wna-"+attr) / counts.get("wn"));
			}


			for(String attr : attrs) {
				for(String property : properties) {
					if(property.endsWith("m")) {
						node.setAttr(property+"1-" + attr.replace("wgt-", ""),
								counts.get(property + "-" + attr));
					}
					else {
						node.setAttr(property + "1-" + attr.replace("wgt-", ""),
								counts.get(property + "a-" + attr));
					}
				}
			}
		}
		//System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " " + "Done.");

		return ret;
	}

	static String[] calculateAttrs(AttributedGraph graph, 
			Collection<AttributedNode> nodes, 
			String[] attrs) {
		if(attrs == null) {
			return firstIteration(graph, nodes);

		}
		if(attrs.length == 0)
			return attrs;

		if(nodes == null)
			nodes = graph.getNodes();

		return nextIteration(graph, nodes, attrs);
	}


	/**
	 * Main class for iteration. Replaces a set of python scripts that post-processed each set
	 * of generated features to make new features. Algorithm:
	 * 
	 * 1) Generate primitive features on each node (degree, egonet edge counts, etc.)
	 * 2) Eliminate redundant features using correlation analysis
	 * 3) Bin remaining features using vertical logarithmic binning
	 * 4) Recalculate compound features (sums and averages) for each node and feature
	 * 5) Repeat from 2 until no new features emerge
	 * 
	 * @param args [1] .csv file with u,v,weight records [2] maximum rank error
	 *  [3] bin fraction [4] feature filename [5] id filename
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void main(String[] args) throws IOException, ParseException{
		String graphFile = args[0];
		int maxDist = new Integer(args[1]).intValue();
		double binSize = new Double(args[2]).doubleValue();
		String featFile = args[3];
		String idFile = args[4];
		String curLine, uid, vid;
		Double weight;
		int numIters = 0;

		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
				" Reading " + graphFile);

//		AttributedGraph graph = null;

		AttributedGraph graph = new AttributedGraph();


		BufferedReader reader = new BufferedReader(new FileReader(graphFile));

		graph.buildNodeIndex("nodeID");
		while ((curLine = reader.readLine()) != null) {
			String[] fields = curLine.split(",");
			uid = new String(fields[0]);
			vid = new String(fields[1]);
			weight = new Double(fields[2]);

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

		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
		" Finished ingesting graph");

		graph.buildNodeIndex("nodeID");

		List<String> allReps = new ArrayList<String>();

		String[] primitives = calculateAttrs(graph, null, null);

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
			String ranks = verticalBin(graph, attr, binSize);
			for (AttributedNode n1 : graph.getNodes()) {
				double rank = (Double)n1.getAttr(ranks);
				n1.setAttr(attr, rank);
				n1.attrs.remove(ranks);
				
				if(!sortedAttrSet.containsKey(rank)) {
					sortedAttrSet.put(rank, new HashSet<AttributedNode>());
				}
				
				sortedAttrSet.get(rank).add(n1);
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
		
		System.out.println(candidates);



		System.out.println("Total covariances to calculate: " +
				candidates.size() + " * " + (candidates.size()) + " = " + 
				(candidates.size()*(candidates.size())));

		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
		" Computing representatives.");
		Set<String> reps = calculateReps(graph, maxDist, candidates, maxBins,
				sortedAttrSets, memoizedMatches);
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Done.");

		System.out.println(reps);
		
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
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
		" Computing vertical bins.");

		for(String rep : reps) {
			allReps.add(rep);
			attrs[i++] = verticalBin(graph, rep, binSize);
		}
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Done.");

		numIters = 1;

		
		int j = 1;
		while(attrs.length > 0 && numIters++ < MAX_ITERATIONS) {
			maxDist += 1;
			System.out.println("Iteration " + (j++) + ". Current features:");
			for(String f : allReps)
				System.out.println(f);
			String[] features = calculateAttrs(graph, null, attrs);

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
				String ranks = verticalBin(graph, attr, binSize);
				for (AttributedNode n1 : graph.getNodes()) {
					double rank = (Double)n1.getAttr(ranks);
					n1.setAttr(attr, rank);
					n1.attrs.remove(ranks);
					
					if(!sortedAttrSet.containsKey(rank)) {
						sortedAttrSet.put(rank, new HashSet<AttributedNode>());
					}
					
					sortedAttrSet.get(rank).add(n1);
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

			
			
			
			System.out.println();

			candidates = new HashSet<String>();
			for (String s : features) {
				if (!constants.contains(s)) {
					candidates.add(s);
				}
			}
			for(String s : allReps) candidates.add(s);

			int numNewFeats = features.length - constants.size();
			System.out.println("Total covariances to calculate: " +
					(numNewFeats) + " * " + 
					(allReps.size() + numNewFeats) + " = " + 
					(numNewFeats*(allReps.size()+numNewFeats)));

			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
			" Computing representatives.");
			reps = calculateReps(graph, maxDist, candidates, maxBins,
					sortedAttrSets, memoizedMatches);
			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Done.");

			System.out.println("Kept " + (reps.size()-allReps.size()) + " of " + numNewFeats);

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
			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
			" Computing vertical bins.");

			for (String rep : reps) {
				if(allReps.contains(rep)) continue;
				allReps.add(rep);
				attrs[i++] = verticalBin(graph, rep, binSize);
			}
			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Done.");

		}

		PrintStream featOut = new PrintStream(featFile);
		PrintStream idOut = new PrintStream(idFile);

		List<AttributedNode> nodes = new ArrayList<AttributedNode>(graph.getNodes());

		try {
			Collections.sort(nodes, 
					new Comparator<AttributedNode>() {
				public int compare(AttributedNode n1, AttributedNode n2) {
					return Integer.parseInt((String)n1.getAttr("nodeID")) - 
							Integer.parseInt((String)n2.getAttr("nodeID"));
				}
			});
		}
		catch(Exception e) {
		Collections.sort(nodes, 
				new Comparator<AttributedNode>() {
			public int compare(AttributedNode n1, AttributedNode n2) {
				return ((String)n1.getAttr("nodeID")).compareTo((String)n2.getAttr("nodeID"));
			}
		});
		}

		for (AttributedNode n : nodes) {
			double sum = 0.0;
			for (String rep : allReps) {
				sum += (Double)n.getAttr(rep);
			}
			//if(sum < 1) continue;
			idOut.println(n.getAttr("nodeID"));
			for(String rep : allReps) {
				featOut.print(n.getAttr(rep) + " ");
			}
			featOut.println(sum > 0?0:1);
		}
	}

	static String verticalBin(AttributedGraph graph, String attr, double binSize) {

		int numNodes = graph.getNodes().size();
		int added = 0;

		class Pair implements Comparable<Pair> {
			double value;
			AttributedNode node;
			Pair(double value, AttributedNode node) {
				this.value = value;
				this.node = node;
			}
			public int compareTo(Pair o) {
				Pair p = (Pair)o;
				return value - p.value < 0?-1:1;
			}
			public String toString() { 
				return node.getAttr("nodeID") + ": " + value;
			}
		}
		List<Pair> values = new ArrayList<Pair>();
		for(AttributedNode n : graph.getNodes()) {
			//if(Math.abs((Double) n.getAttr(attr) - 0) > 1E-5)
				values.add(new Pair((Double)n.getAttr(attr), n));
			//else
			//	n.setAttr("wgt-" + attr, 0.0);
		}
		Collections.sort(values);
		numNodes = values.size();
		if(numNodes == 0) {
			return "wgt-" + attr;
		}
		
		double score = 0;
		int needed = (int)Math.ceil(binSize*(numNodes));
		int thisBin = 0;
		AttributedNode n = values.get(0).node;
		double oldVal = (Double) n.getAttr(attr), newVal;
		n.setAttr("wgt-" + attr, score);
		thisBin++;
		added++;
		while(added < numNodes) {
			n = values.get(added).node;
			newVal = (Double) n.getAttr(attr);
			if(absDiff(newVal,oldVal) > TOLERANCE && thisBin >= needed) {
				score += 1;
				thisBin = 0;
				needed = (int)Math.ceil(binSize*(numNodes-added));
			}
			oldVal = newVal;
			n.setAttr("wgt-" + attr, score);
			added++;
			thisBin++;
		}
		return "wgt-" + attr;
	}

	static Set<String> calculateReps(AttributedGraph graph, 
			int maxDist, Set<String> candidates,
			Map<String, Double> maxBins,
			Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets,
			Map<String, Map<String, Boolean>> memoizedMatches) {
		Set<String> reps = new HashSet<String>(); 
		Map<String, String> p = new HashMap<String, String>();

		
		Set<String> exactMatches = new HashSet<String>();

		for(String attr1 : new HashSet<String>(candidates)) {
			p.put(attr1, attr1);
		}
		
		
		
		for(String attr1 : candidates) {
			for(String attr2 : candidates) {
				if(attr1 == attr2) continue;
				if(smallerString(attr1, attr2).equals(attr2)) continue;
				if(memoizedMatches.containsKey(attr1) && 
						memoizedMatches.get(attr1).containsKey(attr2)) {
					if(memoizedMatches.get(attr1).get(attr2)) {
						union(attr1, attr2, p);
						continue;
					}
					
				}
				corrChecks += graph.getNumNodes();
				
				if(exactMatches.contains(attr2)) continue;
				
				boolean match = attrOrdersAgree(attr1,attr2,graph,maxDist, maxBins,
						sortedAttrSets, memoizedMatches);
				
//				if(attr2.equals("xedm0") && attr1.equals("xem0"))
//					System.out.println();
				
				
				if(!memoizedMatches.containsKey(attr1))
					memoizedMatches.put(attr1, new HashMap<String, Boolean>());
				memoizedMatches.get(attr1).put(attr2, match);
				if(match) {
					union(attr1, attr2, p);
					if(maxDist == 0)
						exactMatches.add(attr2);
				}
			}
		}
		
		//System.out.println(checks + " of " + corrChecks + " (" + checks/corrChecks + ")");
		
		for(String attr : candidates) {
			if(find(attr, p) == attr) {
				reps.add(attr);
			}
		}
		return reps;
	}

	
	private static boolean attrOrdersAgree(String attr1, String attr2, 
			AttributedGraph graph, int maxAllowed, Map<String, Double> maxBins,
			Map<String, Map<Double, Set<AttributedNode>>> sortedAttrSets,
			Map<String, Map<String, Boolean>> memoizedMatches) {
		double index1 = maxBins.get(attr1), index2 = maxBins.get(attr2);
		
	
		while(index1 > index2 && index1 > maxAllowed) {
			for(AttributedNode n1 : sortedAttrSets.get(attr1).get(index1)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr1), (Double)n1.getAttr(attr2));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			index1--;
		}
		
		while (index2 > index1 && index2 > maxAllowed) {
			for(AttributedNode n1 : sortedAttrSets.get(attr2).get(index2)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr2), (Double)n1.getAttr(attr1));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			index2--;
		}
		
		double index = index1;
		while(index > maxAllowed) {
			for(AttributedNode n1 : sortedAttrSets.get(attr1).get(index)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr1), (Double)n1.getAttr(attr2));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			
			for(AttributedNode n1 : sortedAttrSets.get(attr2).get(index)) {
				checks += 1;
				double diff = absDiff((Double)n1.getAttr(attr2), (Double)n1.getAttr(attr1));
				if(diff > maxAllowed && diff - maxAllowed > TOLERANCE) {
					return false;
				}
			}
			index--;
		}
		
		
		return true;
	}
	
	
	
	// Utility functions
	
	private static double absDiff(double x, double y) {
		if (x > y) return x-y;
		return y-x;
	}
	
	private static void link(String x, String y, Map<String, String> p) {
		if(smallerString(x, y) == x) p.put(y, x);
		else p.put(x, y);
	}

	private static String find(String x, Map<String, String> p) {
		if (x != p.get(x))
			p.put(x, find(p.get(x), p));
		return p.get(x);
	}

	private static void union(String x, String y, Map<String, String> p) {
		link(find(x, p), find(y, p), p);
	}



	private static String smallerString(String s1, String s2) {
		if(s1.split("-").length < s2.split("-").length) return s1;
		if(s2.split("-").length < s1.split("-").length) return s2;
		if(s2.contains("-wn0-") && !s1.contains("-wn0-")) return s1;
		if(s1.contains("-wn0-") && !s2.contains("-wn0-")) return s2;
		if(s1.length() < s2.length()) return s1;
		if(s2.length() < s1.length()) return s2;
		for(int i = 0; i < s1.length(); i++) {
			if ((int)s1.charAt(i) < (int)s2.charAt(i)) return s1;
			if ((int)s2.charAt(i) < (int)s1.charAt(i)) return s2;
		}
		return s2;
	}

//	private static String largerString(String s1, String s2) {
//		if(s1.split("-").length < s2.split("-").length) return s2;
//		if(s2.split("-").length < s1.split("-").length) return s1;
//		if(s2.contains("-wn0-")) return s2;
//		if(s1.contains("-wn0-")) return s1;
//		if(s1.length() < s2.length()) return s2;
//		if(s2.length() < s1.length()) return s1;
//		for(int i = 0; i < s1.length(); i++) {
//			if ((int)s1.charAt(i) < (int)s2.charAt(i)) return s2;
//			if ((int)s2.charAt(i) < (int)s1.charAt(i)) return s1;
//		}
//		return s1;
//	}
//
//	private static void printArray(Object[] arr) {
//		System.out.print("[ ");
//		for(Object o : arr) {
//			System.out.print(o);
//			System.out.print(' ');
//		}
//		System.out.println("]");
//	}

}

