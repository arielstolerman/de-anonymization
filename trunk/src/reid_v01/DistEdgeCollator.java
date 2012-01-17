package reid_v01;

import java.io.*;
import java.util.*;

/**
 * Class collates an edge to a node based on the minimum rank order of a reference edge 
 * to a target edge containing the target node. 
 * 
 * @author lever1
 */

public class DistEdgeCollator extends EdgeCollator {

	/********************************************************************************
	 *  Constructor
	 ********************************************************************************/

	public DistEdgeCollator(String baseIn, String baseOut, String featureType) {
		super(baseIn, baseOut, featureType);
	}

	/********************************************************************************
	 * Instance Types
	 ********************************************************************************/
	
	protected class MinVertexScore extends VertexScore {
		
		public MinVertexScore(String name) {
			super(name);
		}

		@Override
		public double getScore() {
			return this.score;
		}

		@Override
		public void addTo(double d) {
			if (this.score > d || 0 == count)
				this.score = d;
			++count;
		}
	}
	
	/********************************************************************************
	 *  Instance Methods
	 ********************************************************************************/

	@Override
	public void collateEdges(String nodeId) throws IOException {
		List<File> guessFiles = this.getGuessFiles(nodeId);

		String user = null, curLine = null;
		String[] split = null;
		BufferedReader reader = null;
		int idx = -1;
		double curScore = -1.0;
		Map<String, VertexScore> scores = new HashMap<String, VertexScore>();
		for (File file : guessFiles) {
			reader = new BufferedReader(new FileReader(file));
			split = file.getName().replaceFirst("[.][^.]+$", "").split("-");
			idx = (nodeId == split[0]) ? 0 : 1;
			while((curLine = reader.readLine()) != null) {
				split = curLine.split(",");
				user = split[idx].trim();
				curScore = Double.parseDouble(split[2]);
				if (!scores.containsKey(user))
					scores.put(user, new MinVertexScore(user));
				scores.get(user).addTo(curScore);
			}
			reader.close();
		}
		
		List<VertexScore> sorted = new ArrayList<VertexScore>(scores.values());
		Collections.sort(sorted);

		System.out.println(nodeId);
		int guess = 1;
		for (VertexScore score : sorted) {
			if (score.getName().equals(nodeId)) {
				System.out.println(String.format("  %d\t%s\t%f", guess, score.getName(), score.getScore()));
				break;
			}
			++guess;
		}
		System.out.println();
	}

}
