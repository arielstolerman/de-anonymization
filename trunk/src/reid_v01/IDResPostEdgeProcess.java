package reid_v01;
import java.io.*;
import java.text.*;

/**
 * Main class for scoring identity resolution results. Prior to running IDResPostProcess, one
 * computes features for each node of a reference graph and a target graph. This class 
 * compares each (or a subset) of the nodes in the target graph to all of the nodes in the
 * reference graph. Reference nodes are sorted according to distance to the target node in
 * question, and the position (including ties) of the same node in the sorted list is noted.
 * 
 * All such positions (across N_tar target nodes) are treated as an empirical probability
 * distribution over the ranks 1-N_ref, and the CDF of this distribution is returned (not
 * normalized). CDFs are reported for the baseline (uniform over 1-N_ref), primitive only,
 * and compound+primitive.
 * 
 * @author hendersk, lever1
 *
 */

public class IDResPostEdgeProcess {
	
	/**
	 * Entry point for post-processing. 
	 * 
	 * @param args -(String[])
	 * 	[0] featureType={local,neighborhood,regional} - the type of features to use
	 *  [1] baseInputFileName - base name for input files. files BASE-target.txt,
	 *  						BASE-reference.txt, and BASE-features.txt must exist. The
	 *  						first two are csv feature files, with the first field of each
	 *  						record being the node name, and subsequent fields being feature
	 *  						values. BASE-features.txt is a csv file with feature names.
	 *  [2] baseOutputFileName - base name for output files
	 *  [3] collationType={dist,order, avgDist, avgOrder} - the type of collation to perform
	 *  [4] numExamples - How many example scores to print to stdout
	 *  [5] [numTargets] - optional number of target edges to test on. Chosen in decreasing
	 *  				   order of feature vector magnitude.
 	 *  [6] [idFile] - optional csv file with one line containing the ids of edges to be
 	 *  			   analyzed.
 	 * @return writes several files with baseoutputfilename. the most important of these is 
 	 * 			base-featuretype-cdf.txt, which contains tab-separated lines and describes the cdf of scores.
 	 * 			that is, each line contains a score s (in increasing order) and the number of
 	 * 			nodes with score <= s. achievable scores start at 1 (perfect guess).
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	
	public static void main(String[] args) throws IOException, ParseException {

		String featureType = null;
		String collationType = null;
		int numTargets = Integer.MAX_VALUE;
		String baseOut = null, baseIn = null;
		String idFile = null;
		int numConf = 0;
		
		/*
		try {
			featureType = args[0];
			if(!featureType.equals("local") &&
					!featureType.equals("neighborhood") &&
					!featureType.equals("regional"))
				throw new RuntimeException();
			baseIn = args[1];
			baseOut = args[2];
			collationType = args[3];
			numConf = new Integer(args[4]).intValue();
			if(args.length > 5) numTargets = new Integer(args[5]).intValue();
			if(args.length > 6) idFile = args[6];
		}
		catch(Exception e) {
			System.err.println("Usage: java IDResPostEdgeProcess {local,neighborhood,regional} " + 
			"baseInputFileName baseOutputFileName {dist,order,avgDist,avgOrder} numExamples [numTargets] [idFile]");
			return;
		}
		*/
		
		// TODO hardcoded
		featureType = "local";
		baseIn = IdentityResolution.base;
		collationType = "avgDist";
		baseOut = IdentityResolution.base+"_post_process_edges_collationType-"+collationType;
		numConf = 3;
		
		// Correlate edges to nodes and score guesses
		EdgeCollator collator = null;
		if ("avgdist".equalsIgnoreCase(collationType)) 
			collator = new AvgDistEdgeCollator(baseIn, baseOut, featureType);
		else if ("avgorder".equalsIgnoreCase(collationType))
			collator = new AvgOrderEdgeCollator(baseIn, baseOut, featureType);
		else if ("dist".equalsIgnoreCase(collationType))
			collator = new DistEdgeCollator(baseIn, baseOut, featureType);
		else if ("order".equalsIgnoreCase(collationType))
			collator = new OrderEdgeCollator(baseIn, baseOut, featureType);
		else
			System.out.println(String.format("%s: collation type does not exist", collationType));
		
		int count = 0;
		if (null != idFile) {
			BufferedReader reader = new BufferedReader(new FileReader(idFile));
			String[] split = reader.readLine().split(",");
			reader.close();
			for (String nodeId : split) {
				if (count++ >= numTargets) break;
				collator.collateEdges(nodeId.trim());
			}
			return;
		}
				
		for (String nodeId : collator.getTargetNodeList()) {
			if (count++ >= numTargets) break;
			collator.collateEdges(nodeId);
		}
	}

}
