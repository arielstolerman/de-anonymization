package analysis;

import java.util.*;

public abstract class BasicGraphMatcher extends GraphMatcher {
	
	/**
	 * Basic graph matching by an implemented comparing method.
	 */
	@Override
	public void match(Graph[] tarClusters, Graph[] refClusters) {
		if (tarClusters.length != refClusters.length) {
			System.err.println("target number of clusters and reference number of clusters is not the same!");
			return;
		}
		
		int n = tarClusters.length;
		Pair<Graph,Graph>[] matches = new Pair[n];
		
		// convert to lists
		List<Graph> tarList = new ArrayList<Graph>(n);
		List<Graph> refList = new ArrayList<Graph>(n);
		for (int i=0; i<n; i++) {
			tarList.add(tarClusters[i]);
			refList.add(refClusters[i]);
		}
		
		// sort by rank - total number of edges
		Comparator<Graph> gc = getGraphComparator();
		Collections.sort(tarList,gc);
		Collections.sort(refList,gc);
		
		for (int i=0; i<n; i++) {
			matches[i] = new Pair<Graph,Graph>(tarList.get(i),refList.get(i));
			tarClusters[i] = tarList.get(i);
			refClusters[i] = refList.get(i);
			System.out.println((i+1)+": target - "+tarList.get(i).edgeCount+" edges and "+tarList.get(i).nodeCount+
					" nodes, reference - "+refList.get(i).edgeCount+" edges and "+refList.get(i).nodeCount+" nodes");
		}
		System.out.println();
	}
	
	public abstract Comparator<Graph> getGraphComparator();
}
