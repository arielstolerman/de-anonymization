package analysis;

import java.io.*;
import java.util.*;

public class Graph implements Serializable {
	
	protected static final long serialVersionUID = -4850267578295753989L;
	
	// nodes
	protected int nodeCount = 0;
	protected Map<String,Integer> nodes = new HashMap<String,Integer>();
	protected Map<Integer,String> nodesInv = new HashMap<Integer,String>();
	
	// edges
	protected int edgeCount = 0;
	protected Map<Integer,List<Pair<Integer,Integer>>> edges = new HashMap<Integer,List<Pair<Integer,Integer>>>();
	

	/* ====================
	 *  add edges and nodes
	 * ====================
	 */
	
	/**
	 * Creates edges and nodes from a given line that includes from-node, to-node and Integer edge-weight,
	 * separated by the given delimiter.
	 * @param line
	 * @param delim
	 */
	public void addEdge(String line, String delim) {
		String[] lineArr = line.split(delim);
		int weight = Double.valueOf(lineArr[2]).intValue();
		addEdge(lineArr[0],lineArr[1],weight);
	}
	
	/**
	 * Creates edges and nodes from a given from-node, to-node and Integer edge-weight.
	 * @param from
	 * @param to
	 * @param weight
	 */
	public void addEdge(String from, String to, int weight) {
		// from node
		Integer fromId = nodes.get(from);
		if (fromId == null) {
			nodeCount++;
			nodes.put(from, nodeCount);
			nodesInv.put(nodeCount, from);
			fromId = nodeCount;
		}
		
		// to node
		Integer toId = nodes.get(to);
		if (toId == null) {
			nodeCount++;
			nodes.put(to, nodeCount);
			nodesInv.put(nodeCount, to);
			toId = nodeCount;
		}
		
		// add edge
		List<Pair<Integer,Integer>> adj = edges.get(fromId.intValue());
		if (adj == null) {
			adj = new ArrayList<Pair<Integer,Integer>>();
			edges.put(fromId.intValue(), adj);
		}
		adj.add(new Pair<Integer,Integer>(toId,weight));
		edgeCount++;
	}
	
	/**
	 * Adds edges and maps nodes by their original mapping in the graph.
	 * Here the given "from" and "to" are already the Integer ids given in the original mapping.
	 * @param from
	 * @param to
	 * @param weight
	 */
	public void addEdgeMappedNodes(Map<String,Integer> originalNodes, Map<Integer,String> originalNodesInv, int from, int to, int weight) {
		// from node
		String fromId = nodesInv.get(from);
		if (fromId == null) {
			fromId = originalNodesInv.get(from);
			nodesInv.put(from, fromId);
			nodes.put(fromId, from);
			nodeCount++;
		}
		
		// to node
		String toId = nodesInv.get(to);
		if (toId == null) {
			toId = originalNodesInv.get(to);
			nodesInv.put(to, toId);
			nodes.put(toId, to);
			nodeCount++;
		}
		
		// add edge
		List<Pair<Integer,Integer>> adj = edges.get(from);
		if (adj == null) {
			adj = new ArrayList<Pair<Integer,Integer>>();
			edges.put(from, adj);
		}
		adj.add(new Pair<Integer,Integer>(to,weight));
		edgeCount++;
	}
	
	
	/* ==================
	 * additional methods
	 * ================== 
	 */
	
	/**
	 * Sorts all adjacency lists
	 */
	public void sortEdgeLists() {
		for (List<Pair<Integer,Integer>> adj: edges.values()) {
			Collections.sort(adj, new Comparator<Pair<Integer,Integer>>() {
				@Override
				public int compare(Pair<Integer, Integer> o1, Pair<Integer, Integer> o2) {
					return o1.first - o2.first;
				}
			});
		}
	}
	
	/* ==============
	 * output methods
	 * ==============
	 */
	
	public String toString() {
		String res = "";
		
		res += "nodes: "+nodeCount+", edges: "+edgeCount+"\n";
		res +="\n";
		res += "nodes:\n";
		for (String node: nodes.keySet()) {
			res += node+" -> "+nodes.get(node)+"\n";
		}
		res +="\n";
		res += "edges:\n";
		for (Integer node = 1; node <= nodeCount; node++) {
			res += node+": ";
			List<Pair<Integer,Integer>> adj = edges.get(node);
			if (adj == null) {
				res += "none";
			} else {
				for (Pair<Integer,Integer> value: adj) {
					res += "("+value.first+", "+value.second+") ";
				}
			}
			res += "\n";
		}
		
		return res;
	}
	
	/**
	 * returns the graph in Metis format.
	 */
	public String toMetisString() {
		String res = "";
		
		// number of vertices and edges
		res += nodeCount+" "+edgeCount+"\n";
		
		// adjacency lists
		Integer node;
		for (node = 1; node <= nodeCount; node++) {
			List<Pair<Integer,Integer>> adj = edges.get(node);
			if (adj != null) {
				for (Pair<Integer,Integer> value: adj) {
					res += value.first+" "+value.second+" ";
				}
			}
			res += "\n";
		}
		res += "\n";
		System.out.println("total nodes: "+(node-1));
		
		// remove last newline
		res = res.substring(0,res.length()-1);
		
		return res;
	}
	
	/**
	 * Returns the graph in CSV format (list of edges) with the original node ids.
	 */
	public String toCSVString() {
		String res = "";
		
		Integer node;
		for (node = 1; node <= nodeCount; node++) {
			List<Pair<Integer,Integer>> adj = edges.get(node);
			String nodeStr = nodesInv.get(node.intValue());
			if (adj != null) {
				for (Pair<Integer,Integer> value: adj) {
					res += nodeStr+","+nodesInv.get(value.first.intValue())+","+value.second+"\n";
				}
			}
		}
		System.out.println("total nodes: "+(node-1));
		
		return res;
	}
	
	/**
	 * Writes the graph in Metis format to the given path.
	 * @param path
	 */
	public void writeToMetis(String path) throws Exception {
		System.out.println("creating METIS file from graph");
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		System.out.println("creating scanner...");
		Scanner scan = new Scanner(new StringReader(toMetisString()));
		System.out.println("writing to file: "+path);
		int lineCount = 0;
		while(scan.hasNext()) {
			bw.write(scan.nextLine()+"\n");
			lineCount++;
			//if (scan.hasNext())
			//	bw.write("\n");
			if (lineCount % 100 == 0) {
				bw.flush();

			}
		}
		bw.flush();
		bw.close();
		scan.close();
		System.out.println("wrote "+lineCount+" lines");
	}
	
	/**
	 * writes the graph in CSV format (list of edges).
	 * @param path
	 */
	public void writeToCSV(String path) throws Exception {
		System.out.println("creating CSV file from graph");
		BufferedWriter bw = new BufferedWriter(new FileWriter(path));
		System.out.println("creating scanner...");
		Scanner scan = new Scanner(new StringReader(toCSVString()));
		System.out.println("writing to file: "+path);
		int lineCount = 0;
		while(scan.hasNext()) {
			bw.write(scan.nextLine());
			lineCount++;
			if (scan.hasNext())
				bw.write("\n");
			if (lineCount % 100 == 0) {
				bw.flush();
			}
		}
		bw.flush();
		bw.close();
		scan.close();
		System.out.println("wrote "+lineCount+" lines");
	}
	
}


/*
/**
 * Removes all unused nodes from the node map.
 */
/*
public void removeUnusedNodes() {
	System.out.println("removing unused nodes from node map. initial size: "+nodes.size());
	
	// invert map
	Set<Integer> inv = new TreeSet<Integer>();
	for (String key: nodes.keySet())
		inv.add(nodes.get(key));
	
	int removeCount = 0;
	
	// iterate over all nodes and remove unused ones
	for (Integer node: inv) {
		// first check if it's a source node
		if (edges.keySet().contains(node.intValue()))
			continue;
		
		// if not, check if it's a target node
		boolean found = false;
		for (Integer src: edges.keySet()) {
			List<Pair<Integer,Integer>> adj = edges.get(src.intValue());
			if (adj != null) {
				// iterate over all adjacent nodes and check if it's one of them
				for (Pair<Integer,Integer> value: adj) {
					if (value.first.intValue() == node.intValue()) {
						found = true;
						break;
					}
				}
			}
			if (found) break;
		}
		
		// if did not find, remove node
		if (!found) {
			nodes.remove(node);
			removeCount++;
		}
	}
	
	System.out.println("removed "+removeCount+" nodes. new size: "+nodes.size());
	nodeCount = nodes.size();
	System.out.println("updated nodeCount");
	System.out.println();
}
*/