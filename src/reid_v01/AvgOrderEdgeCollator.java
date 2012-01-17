package reid;

import java.io.*;
import java.util.*;

/**
 * Class collates an edge to a node based on the average ordering of the reference 
 * edge in the guess files for a particular target node. These files are sorted from
 * smallest to largest distance from target edge to reference edge.
 * 
 * @author lever1
 */

public class AvgOrderEdgeCollator extends EdgeCollator {

	/********************************************************************************
	 *  Constructor
	 ********************************************************************************/

	public AvgOrderEdgeCollator(String baseIn, String baseOut, String featureType) {
		super(baseIn, baseOut, featureType);
	}

	/********************************************************************************
	 * Instance Types
	 ********************************************************************************/
	
	protected class AvgVertexScore extends VertexScore {
		protected double avg = -1.0;
		
		public AvgVertexScore(String name) {
			super(name);
		}

		@Override
		public double getScore() {
			return (this.avg < 0.0) ? (this.avg = this.score / this.count) : this.avg;
		}
	
		@Override
		public void addTo(double d) {
			super.addTo(d);
			this.avg = -1.0;
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
		int idx = -1, rank = -1;
		Map<String, VertexScore> scores = new HashMap<String, VertexScore>();
		for (File file : guessFiles) {
			reader = new BufferedReader(new FileReader(file));
			split = file.getName().replaceFirst("[.][^.]+$", "").split("-");
			idx = (nodeId == split[0]) ? 0 : 1;
			rank = 1;
			while((curLine = reader.readLine()) != null) {
				split = curLine.split(",");
				user = split[idx].trim();
				if (!scores.containsKey(user))
					scores.put(user, new AvgVertexScore(user));
				scores.get(user).addTo((double)rank++);
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
