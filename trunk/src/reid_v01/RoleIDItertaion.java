package reid;

import java.io.*;
import java.text.*;
import java.util.*;


public class RoleIDItertaion {

	private static final int MAX_ITERATIONS = 6;
	

	private static Map<String, Double> means = new HashMap<String, Double>();
	private static Map<String, Double> stdevs = new HashMap<String, Double>();
	private static Map<String, Double> covars = new HashMap<String, Double>();
	private static Map<String, Double> rhos = new HashMap<String, Double>();

	private static String[] firstIteration(AttributedGraph graph, Collection<AttributedNode> nodes) {
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
		//EgonetGenerator egoGen = new EgonetGenerator(graph, new String[]{"type"}, new String[]{"follows"}, null);
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Computing primitives.");
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

		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " " + "Done.");

		return ret;
	}

	private static String[] nextIteration(AttributedGraph graph, Collection<AttributedNode> nodes,
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
				ret[(k++) + properties.length*attrs.length] = property + "1-" + attr.replace("wgt-", "");
			}	
		}

		EgonetGenerator egoGen = new EgonetGenerator(graph, null, null, null, attrs);
		//EgonetGenerator egoGen = new EgonetGenerator(graph, new String[]{"type"}, new String[]{"follows"}, null);
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
				" Computing compound features.");
		for(AttributedNode node : nodes) {
			Map<String, Double> counts; 

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

		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " " + "Done.");

		return ret;
	}

	private static String[] calculateAttrs(AttributedGraph graph, Collection<AttributedNode> nodes, String[] attrs) {
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
 * @param args [1] .csv file with u,v,weight records [2] correlation 
 * 		threshold [3] bin fraction [4] feature filename [5] id filename
 * @throws IOException
 * @throws ParseException
 */
	public static void main(String[] args) throws IOException, ParseException{
		String graphFile = args[0];
		double corrProb = new Double(args[1]).doubleValue();
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
		Set<String> candidates = new HashSet<String>();
		for (String s : primitives) candidates.add(s);
		

		System.out.println("Total covariances to calculate: " +
				primitives.length + " * " + (primitives.length) + " = " + 
				(primitives.length*(primitives.length)));
		
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
				" Computing representatives.");
		Set<String> reps = calculateReps(graph, corrProb, candidates);
		System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Done.");

		for(String attr : candidates) {
			if(allReps.contains(attr) || reps.contains(attr)) {
				reps.add(attr);
			}
			else {
				means.remove(attr);
				stdevs.remove(attr);
				for(String attr2 : candidates) {
					String key = smallerString(attr, attr2) + " " + largerString(attr, attr2);
					covars.remove(key);
					rhos.remove(key);
				}
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
		while(attrs.length > 0 && numIters++ <= MAX_ITERATIONS) {
			System.out.println("Iteration " + (j++) + ". Current features:");
			for(String f : allReps)
				System.out.println(f);
			String[] features = calculateAttrs(graph, null, attrs);
			candidates = new HashSet<String>();
			for(String s : features) candidates.add(s);
			for(String s : allReps) candidates.add(s);
			
			System.out.println("Total covariances to calculate: " +
					features.length + " * " + (allReps.size() + features.length) + " = " + 
					(features.length*(allReps.size()+features.length)));
			
			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + 
					" Computing representatives.");
			reps = calculateReps(graph, corrProb, candidates);
			System.out.println(TimeUtils.dateAsString(System.currentTimeMillis()) + " Done.");
			
			
			for(String attr : candidates) {
				if(allReps.contains(attr) || reps.contains(attr)) {
					reps.add(attr);
				}
				else {
					means.remove(attr);
					stdevs.remove(attr);
					for(String attr2 : candidates) {
						String key = smallerString(attr, attr2) + " " + largerString(attr, attr2);
						covars.remove(key);
						rhos.remove(key);
					}
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
		
		Collections.sort(nodes, 
				new Comparator<AttributedNode>() {
			public int compare(AttributedNode n1, AttributedNode n2) {
				return ((String)n1.getAttr("nodeID")).compareTo((String)n2.getAttr("nodeID"));
			}
		});
		
		for (AttributedNode n : nodes) {
			idOut.println(n.getAttr("nodeID"));
			for(String rep : allReps) {
				if(rep != allReps.get(allReps.size()-1))
					featOut.print(n.getAttr(rep) + " ");
				else
					featOut.print(n.getAttr(rep));
			}
			featOut.println();
		}
	}

	private static String verticalBin(AttributedGraph graph, String attr, double binSize) {
		
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
			values.add(new Pair((Double)n.getAttr(attr), n));
		}
		Collections.sort(values);

		double score = 0.0;
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
			if(newVal != oldVal && thisBin >= needed) {
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

	private static Set<String> calculateReps(AttributedGraph graph, 
			double corrProb, Set<String> candidates) {
		Set<String> reps = new HashSet<String>(); 
		Map<String, String> p = new HashMap<String, String>();


		for(String attr1 : new HashSet<String>(candidates)) {
			if(getStDev(graph, attr1) == 0) {
				candidates.remove(attr1);
				stdevs.remove(attr1);
				means.remove(attr1);
			}
			else
				p.put(attr1, attr1);
		}
		for(String attr1 : candidates)
			for(String attr2 : candidates)
				getRho(graph, attr1, attr2);

		for(String s : rhos.keySet()) {
			if(rhos.get(s) > corrProb) {
				String[] spl = s.split(" ");
				union(spl[0], spl[1], p);
			}
		}
		
		for(String attr : candidates) {
			if(find(attr, p) == attr) {
				reps.add(attr);
			}
		}
		return reps;
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

	private static double getMean(AttributedGraph graph, String attr) {
		if(means.get(attr) == null) {
			double sum = 0, cnt = 0;
			for (AttributedNode n : graph.getNodes()) {
				try {
					sum += (Double) n.attrs.get(attr);
				}
				catch (NullPointerException e) {
					System.out.println(attr);
					System.out.println(n.attrs);
					throw(e);
				}
				cnt++;
			}
			means.put(attr, sum/cnt);
		}
		return means.get(attr);
	}

	private static double getStDev(AttributedGraph graph, String attr) {
		if(stdevs.get(attr) == null) {
			double mean = getMean(graph, attr);
			double sum = 0, cnt = 0, diff;
			for (AttributedNode n : graph.getNodes()) {
				diff = ((Double) n.attrs.get(attr) - mean);
				sum += diff*diff;
				cnt++;
			}
			stdevs.put(attr, Math.sqrt(sum/cnt));
		}
		return stdevs.get(attr);
	}

	private static double getCovar(AttributedGraph graph, 
			String attr1, String attr2) {
		String key = smallerString(attr1,attr2) + " " + largerString(attr1,attr2);
		if (covars.get(key) == null) {
			double psum = 0, cnt = 0, v1, v2;
			for (AttributedNode n : graph.getNodes()) {
				v1 = (Double) n.attrs.get(attr1);
				v2 = (Double) n.attrs.get(attr2);
				psum += v1*v2;
				cnt++;
			}
			covars.put(key, psum/cnt - getMean(graph, attr1)*getMean(graph, attr2));
		}
		return covars.get(key);
	}

	private static double getRho(AttributedGraph graph, 
			String attr1, String attr2) {
		String key = smallerString(attr1,attr2) + " " + largerString(attr1,attr2);
		if (rhos.get(key) == null) {
			rhos.put(key, 
					getCovar(graph, attr1, attr2) / getStDev(graph, attr1) / getStDev(graph, attr2));
		}
		return rhos.get(key);
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

	private static String largerString(String s1, String s2) {
		if(s1.split("-").length < s2.split("-").length) return s2;
		if(s2.split("-").length < s1.split("-").length) return s1;
		if(s2.contains("-wn0-")) return s2;
		if(s1.contains("-wn0-")) return s1;
		if(s1.length() < s2.length()) return s2;
		if(s2.length() < s1.length()) return s1;
		for(int i = 0; i < s1.length(); i++) {
			if ((int)s1.charAt(i) < (int)s2.charAt(i)) return s2;
			if ((int)s2.charAt(i) < (int)s1.charAt(i)) return s1;
		}
		return s1;
	}

}

