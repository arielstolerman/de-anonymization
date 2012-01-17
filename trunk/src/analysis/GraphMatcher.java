package analysis;

import java.util.*;

public abstract class GraphMatcher {
	
	/**
	 * Matches clusters from the target graph to clusters from the reference graph.
	 * The input target and reference graphs should be sorted by match.
	 * @param tar
	 * @param ref
	 * @return
	 */
	public abstract void match(Graph[] tarClusters, Graph[] refClusters);
}
