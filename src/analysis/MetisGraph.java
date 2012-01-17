package analysis;

import java.io.*;
import java.util.*;
import reid.*;

public class MetisGraph {
	
	/**
	 * Entry point for MetisGraph - for creating METIS compatible graphs from given CSV format graphs
	 * and creating mapping from node id to cluster after running METIS to cluster the graphs.
	 * @param args
	 * args[0] - should be either of "create_metis_files" or "create_cluster_map" for phase 1 and phase 2 respectively.
	 * args[1] - the path to the graph file.
	 * args[3] - number of clusters in the partitioning.
	 */
	public static void main(String[] args) {
		if (args.length != 3)
			exitWithMsg();
		
		// task
		String task = args[0];
		if (!task.equals("create_metis_files") && !task.equals("create_cluster_map")) {
			System.out.println("args[0] should be either of \"create_metis_files\" or \"create_cluster_map\" for phase 1 and phase 2 respectively; given: "+task);
			exitWithMsg();
		}
		
		// target path
		String graphPath = args[1];
		try {
			File f = new File(graphPath);
		} catch (Exception e) {
			System.out.println("Failed reading graph file: "+graphPath);
			exitWithMsg();
		}
		
		// number of clusters
		int numClusters = 0;
		try {
			numClusters = Integer.parseInt(args[2]);
		} catch (NumberFormatException e) {
			System.out.println("Failed parsing number of clusters from: "+args[2]);
			exitWithMsg();
		}
		
		// run task
		if (task.equals("create_metis_files"))
			createMetisFiles(graphPath);
		else if (task.equals("create_cluster_map"))
			createClusterMappingFiles(graphPath, numClusters);
	}
	
	public static void exitWithMsg() {
		System.out.println("Usage:");
		System.out.println("java -jar metis.jar <create_metis_files|create_cluster_map> /path/to/graph num-of-clusters");
		System.out.println("exiting...");
		System.exit(1);
	}
	
	/**
	 * Phase 1 of 2:
	 * =============
	 * - creates graph objects from the given graph
	 * - writes graph to a binary file for reading in phase 2
	 * - writes the graph in metis format 
	 */
	public static void createMetisFiles(String graphPath) {

		Graph g = null;
		
		// create graph
		try {
			g = createGraph(graphPath, ",", -1);
		} catch (Exception e) {
			System.err.println("Failed to create graph from path: "+graphPath);
			e.printStackTrace();
			System.exit(1);
		}
		
		// write graph to binary file
		try {
			writeGraphToBinary(g, graphPath);
		} catch (Exception e) {
			System.err.println("Failed to write graph to binary file: "+graphPath);
			e.printStackTrace();
		}
		
		// create METIS format file
		try {
			createMetisFile(g,graphPath);
		} catch (Exception e) {
			System.err.println("Failed to create METIS format graph: "+graphPath);
			e.printStackTrace();
		}
		
		g = null;
		System.gc();
	}
	
	/**
	 * Phase 2 of 2:
	 * =============
	 * - reads graph from binary files
	 * - creates node to cluster mapping
	 */
	public static void createClusterMappingFiles(String graphPath, int numClusters) {
		
		Graph g = null;
		
		// read graph from binary
		try {
			g = readGraphFromBinaryFile(graphPath);
		} catch (Exception e) {
			System.err.println("Failed to read graph from binary file: "+graphPath);
			e.printStackTrace();
		}
		
		// write mapping
		try {
			createNodeToClusterMapping(g,graphPath,numClusters);
		} catch (Exception e) {
			System.err.println("Failed to create node-to-cluster mapping: "+graphPath);
			e.printStackTrace();
		}

		g = null;
		System.gc();
	}
	
	
	/*
	 * =======================
	 * main for parsing graphs
	 * =======================
	 */
	/*
	public static void main(String[] args) throws Exception {
		// reduced sms-wire data
		//String tarGraph = "./data/att-reduced-deg/sms";
		//String refGraph = "./data/att-reduced-deg/wire";
		
		// full data
		String tarGraph = "./data/att/allsms-anon.2011-09";
		String refGraph = "./data/att/allawsv-anon.2011-09";
		
		String delim = ",";
		int limit = -1;
		Graph tar;
		Graph ref;
		int numClusters = 10;
		int numDigits = 2;
		
		/* ========================================================================================
		 * 										create graphs
		 * ========================================================================================
		 */
		
		/* ==================================
		 * create reduced graphs binary files
		 * ================================== 
		 */
		
		//tar = createGraph(tarGraph, delim, limit);
		//ref = createGraph(refGraph, delim, limit);
		
		/* ============================
		 * write graphs to binary files
		 * ============================
		 */
		
		//writeGraphToBinary(tar, tarGraph);
		//writeGraphToBinary(ref, refGraph);
		
		/* =============================
		 * read graphs from binary files
		 * =============================
		 */
		
		//tar = readGraphFromBinaryFile(tarGraph);
		//ref = readGraphFromBinaryFile(refGraph);
		
		/* ============================
		 * write graphs in Metis format
		 * ============================
		 */
		
		//createMetisFile(tar,tarGraph);
		//createMetisFile(ref, refGraph);
		
		/* ==========================
		 * write graphs in CSV format
		 * ==========================
		 */
		
		//createCSVFile(tar,tarGraph);
		//createCSVFile(ref,refGraph);
		
		/* ========================================================================================
		 * 									create clusters (sub graphs)
		 * ========================================================================================
		 */
		
		/* ========================================
		 * create files for node to cluster mapping
		 * ========================================
		 */
		//createNodeToClusterMapping(tar,tarGraph,numClusters);
		//createNodeToClusterMapping(ref,refGraph,numClusters);
		
		/* =================================
		 * create arrays of clustered graphs
		 * =================================
		 */
		/*
		Graph[] tarClusters = createClusterGraphs(tar,tarGraph,numClusters);
		Graph[] refClusters = createClusterGraphs(ref,refGraph,numClusters);
		
		/* ==============
		 * match clusters
		 * ============== 
		 */
		/*
		//(new EdgeGraphMatcher()).match(tarClusters, refClusters);
		(new NodeGraphMatcher()).match(tarClusters, refClusters);
		
		/* ===========================
		 * write clusters to csv files
		 * ===========================
		 */
		/*
		for (int i=0; i<numClusters; i++) {
			System.out.println("match "+(i+1)+" out of "+numClusters+":");
			System.out.println("====================================");
			createCSVFile(tarClusters[i], tarGraph+"_"+String.format("%0"+numDigits+"d", (i+1))+"_of_"+numClusters);
			createCSVFile(refClusters[i], refGraph+"_"+String.format("%0"+numDigits+"d", (i+1))+"_of_"+numClusters);
		}
		
		/* =====================================================
		 * run IdentityResolution on each of the cluster matches
		 * =====================================================
		 */
		/*
		String tarCluster;
		String refCluster;
		for (int i=2; i<numClusters; i++) {
			System.out.println("starting identity resolution for cluster pair "+(i+1)+" out of "+numClusters);
			System.out.println("==========================================================================");
			
			// cluster names
			tarCluster = tarGraph+"_"+String.format("%0"+numDigits+"d", (i+1))+"_of_"+numClusters+"_csv.txt";
			refCluster = refGraph+"_"+String.format("%0"+numDigits+"d", (i+1))+"_of_"+numClusters+"_csv.txt";
			
			// run IdRes
			try {
				IdentityResolution.resetStaticVars();
				IdentityResolution.main(new String[]{
						tarCluster,
						refCluster,
						"0",
						"0.5",
						"./data/att-reduced-deg/sms_wire_clusters_"+String.format("%0"+numDigits+"d",(i+1))+"_of_"+numClusters
				});
			} catch (Exception e) {
				System.err.println("IdentityResolution failed for cluster pair "+(i+1)+". Continuing to next pair...");
				e.printStackTrace();
			}
			
			System.out.println();
		}
	}
	*/
	
	// ====================================================================================================================
	// ====================================================================================================================
	
	/**
	 * Creates a reduced graph from the given graph filepath (without extension),
	 * by eliminating all nodes with in / out degree less than the given minimums.
	 * writes the graph into a binary file.
	 * returns the created graph.
	 */
	public static Graph createGraph(String graph, String delim, int limit) throws Exception {
		Scanner scan;
		String line;
		Graph g = new Graph();
		
		/* =================
		 * print information
		 * =================
		 */
		System.out.println("starting with:");
		System.out.println("graph: "+graph);
		System.out.println("delimiter: "+delim);
		System.out.println("limit lines to read from graph-file: "+(limit == -1 ? "none" : limit));
		
		
		/* ====================
		 * read graph from file
		 * ====================
		 */
		System.out.println("reading graph "+graph+"...");
		scan = new Scanner(new FileReader(graph));
		while (scan.hasNext()) {
			line = scan.nextLine();
			g.addEdge(line,delim);
			
			if (limit > 0 && g.edgeCount > limit)
				break;
		}
		scan.close();
		g.sortEdgeLists();
		System.out.println("done! graph contains "+g.nodeCount+" nodes and "+g.edgeCount+" edges");
		
		return g;
	}
	
	/**
	 * Writes the given graph to a binray file with the base as prefix and ".dat" extention.
	 * @param base
	 * @throws Exception
	 */
	public static void writeGraphToBinary(Graph g, String base) throws Exception {
		System.out.println("writing graph to binary file...");
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(base+".dat"));
		out.writeObject(g);
		out.close();
		System.out.println("done!");
		System.out.println();
	}
	
	/**
	 * Reads the graph from the given path (without extension) and returns the graph.
	 */
	public static Graph readGraphFromBinaryFile(String base) throws Exception {
		System.out.println("reading graph "+base+" from binary file...");
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(base+".dat"));
		Graph g = (Graph) in.readObject();
		in.close();
		System.out.println("done!");
		return g;
	}
	
	/**
	 * creates a Metis format graph file.
	 */
	public static void createMetisFile(Graph g, String base) throws Exception {
		System.out.println("writing graph "+base+" in Metis format...");
		g.writeToMetis(base+"_metis.txt");
		System.out.println("done!");
		System.out.println();
	}
	
	/**
	 * creates a CSV format graph file.
	 */
	public static void createCSVFile(Graph g, String base) throws Exception {
		System.out.println("writing graph "+base+" in CSV format...");
		g.writeToCSV(base+"_csv.txt");
		System.out.println("done!");
		System.out.println();
	}
	
	/**
	 * Creates a file that maps each original node id to the cluster it belongs to.
	 * @param g
	 * @param base
	 * @param numClusters
	 * @throws Exception
	 */
	public static void createNodeToClusterMapping(Graph g, String base, int numClusters) throws Exception {
		String clusterMapFile = base+"_metis.txt.part."+numClusters;
		// map nodes to cluster ids
		System.out.println("creating node-to-cluster map file...");
		Scanner clusterScan = new Scanner(new FileReader(clusterMapFile));
		Map<String,Integer> nodeToCluster = new HashMap<String,Integer>(g.nodeCount);
		int node = 1;
		while (clusterScan.hasNext()) {
			nodeToCluster.put(g.nodesInv.get(node), Integer.valueOf(clusterScan.nextLine()));
			node++;
		}
		System.out.println("done! map size: "+nodeToCluster.size());
		
		System.out.println("writing map to file...");
		String out = base+"_metis_nodes_to_clusters_map_"+numClusters+".txt";
		BufferedWriter bw = new BufferedWriter(new FileWriter(out));
		int i = 0;
		for (String key: nodeToCluster.keySet()) {
			i++;
			bw.write(key+","+nodeToCluster.get(key)+"\n");
			if (i % 100 == 0)
				bw.flush();
		}
		bw.flush();
		bw.close();
		System.out.println("done! wrote file "+out);
		System.out.println();
	}
	
	/**
	 * Creates an array of graphs from the given graph. Should have a <graph_name>_metis.txt.part.<numClusters> file.
	 */
	public static Graph[] createClusterGraphs(Graph g, String base, int numClusters) throws Exception {
		Graph[] clusters = new Graph[numClusters];
		for (int i=0; i<numClusters; i++)
			clusters[i] = new Graph();
		
		//String metisGraph = graph+"_metis.txt";
		String clusterMapFile = base+"_metis.txt.part."+numClusters;
		
		// map nodes to cluster ids
		System.out.println("creating node-to-cluster map...");
		Scanner clusterScan = new Scanner(new FileReader(clusterMapFile));
		Map<Integer,Integer> nodeToCluster = new HashMap<Integer,Integer>(g.nodeCount);
		Integer node = 1;
		while (clusterScan.hasNext()) {
			nodeToCluster.put(node, Integer.valueOf(clusterScan.nextLine()));
			node++;
		}
		System.out.println("done! map size: "+nodeToCluster.size());
		
		// add nodes and their edges to corresponding cluster
		System.out.println("adding nodes to cluster graphs...");
		List<Pair<Integer,Integer>> adj;
		for (int i=1; i<=g.nodeCount; i++) {
			adj = g.edges.get(i);
			if (adj != null && !adj.isEmpty()) {
				for (Pair<Integer,Integer> value: adj) {
					clusters[nodeToCluster.get(i).intValue()].addEdgeMappedNodes(g.nodes,g.nodesInv,i, value.first, value.second);
				}
			}
		}
		System.out.println("done! cluster sizes:");
		int sumNodes = 0;
		for (int i=0; i<numClusters; i++) {
			System.out.println("- cluster "+(i+1)+": "+clusters[i].nodeCount+" nodes, "+clusters[i].edgeCount+" edges");
			sumNodes += clusters[i].nodeCount;
		}
		System.out.println("total nodes: "+sumNodes);
		System.out.println();
		
		return clusters;
	}
	
	public static void matchClusters(String tarPrefix, String refPrefix, String pairsOutpath) throws Exception {
		//TODO
	}
	
	/**
	 * Writes the node maps of the graph to a binary file.
	 * @param g
	 * @param base
	 * @throws Exception
	 */
	public static void writeNodeMapToBinary(Graph g, String base) throws Exception {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(base+"_node-map.dat"));
		out.writeObject(g.nodes);
		out.flush();
		out.writeObject(g.nodesInv);
		out.flush();
		out.close();
	}
	
	/**
	 * Reads the node maps of the given graph prefix path TODO
	 * @param base
	 * @return
	 * @throws Exception
	 */
	public static Pair<Map<String,Integer>,Map<Integer,String>> readNodeMapFromBinary(String base) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(base+"_node-map.dat"));
		Map<String,Integer> nodes = (Map<String,Integer>) in.readObject();
		Map<Integer,String> nodesInv = (Map<Integer,String>) in.readObject();
		return new Pair<Map<String,Integer>,Map<Integer,String>>(nodes,nodesInv);
	}
}


/* ==========================
 * calculate in / out degrees
 * ========================== 
 */
/*
System.out.println("calculating node degrees for graph reduction...");
Map<String,Integer> outDeg = new HashMap<String,Integer>();
Map<String,Integer> inDeg = new HashMap<String,Integer>();
scan = new Scanner(new FileReader(graph+".txt"));
while (scan.hasNext()) {
	lineArr = scan.nextLine().split(delim);
	// out degree
	if (outDeg.get(lineArr[0]) == null) {
		outDeg.put(lineArr[0], 1);
	} else {
		outDeg.put(lineArr[0], outDeg.get(lineArr[0])+1);
	}
	// in degree
	if (inDeg.get(lineArr[1]) == null) {
		inDeg.put(lineArr[1], 1);
	} else {
		inDeg.put(lineArr[1], inDeg.get(lineArr[1])+1);
	}
}
scan.close();
System.out.println("done!");
*/

/*
if (outDeg.get(lineArr[0]) != null && outDeg.get(lineArr[0]) >= minOutDeg &&
		outDeg.get(lineArr[1]) != null && outDeg.get(lineArr[1]) >= minOutDeg &&
		inDeg.get(lineArr[0]) != null && inDeg.get(lineArr[0]) >= minInDeg &&
		inDeg.get(lineArr[1]) != null && inDeg.get(lineArr[1]) >= minInDeg)
*/

/*
/**
 * See createGraphHelper.
 * The starting graph is an empty graph.
 * The target graph should be built based on this method.
 * @param graph
 * @param delim
 * @param limit
 * @return
 * @throws Exception
 */
/*
public static Graph createGraph(String graph, String delim, int limit) throws Exception {
	return createGraphHelper(new Graph(), graph, delim, limit);
}

/**
 * See createGraphHelper.
 * The starting graph is a graph with node mapping based on the given graph.
 * The reference graph should be built using this method (based on the target graph).
 * @param initialGraph
 * @param graph
 * @param delim
 * @param limit
 * @return
 * @throws Exception
 */
/*
public static Graph createGraphWithInitialNodes(Graph initialGraph, String graph, String delim, int limit) throws Exception {
	Graph g = new Graph();
	g.nodes = new HashMap<String, Integer>(initialGraph.nodes.size());
	for (String key: initialGraph.nodes.keySet()) {
		g.nodes.put(key, initialGraph.nodes.get(key));
	}
	return createGraphHelper(g,graph,delim,limit);
}

*/




















