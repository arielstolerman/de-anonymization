package analysis;

import java.util.*;

public class NodeGraphMatcher extends BasicGraphMatcher {

	/**
	 * For comparing graphs by number of edges.
	 */
	@Override
	public Comparator<Graph> getGraphComparator() {
		return new Comparator<Graph>() {
			@Override
			public int compare(Graph o1, Graph o2) {
				return o1.nodeCount - o2.nodeCount;
			}
		};
	}
}
