package reid;

import java.io.*;
import java.text.*;

public class GenerateEdgeGuesses {

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
	 *  [3] [numTargets] - optional number of target edges to test on. Chosen in decreasing
	 *  				   order of feature vector magnitude.
 	 *  [4] [idFile] - optional csv file with one line containing the ids of edges to be
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
		int numTargets = Integer.MAX_VALUE;
		String baseOut = null, baseIn = null;
		String idFile = null;
		/*
		try {
			featureType = args[0];
			if(!featureType.equals("local") &&
					!featureType.equals("neighborhood") &&
					!featureType.equals("regional"))
				throw new RuntimeException();
			baseIn = args[1];
			baseOut = args[2];
			if(args.length > 3) numTargets = new Integer(args[3]).intValue();
			if(args.length > 4) idFile = args[4];
		}
		catch(Exception e) {
			System.err.println(String.format("Usage: java %s {local,neighborhood,regional} " + 
			"baseInputFileName baseOutputFileName [numTargets] [idFile]", args[0]));
			return;
		}
		*/
		
		// TODO harcoded
		featureType = "local";
		baseIn = IdentityResolution.base;
		baseOut = IdentityResolution.base;
		
		// Initialize the edge guesser
		EdgeGuesser guesser = new EdgeGuesser(baseIn, baseOut, featureType);

		if (null != idFile) {
			BufferedReader reader = new BufferedReader(new FileReader(idFile));
			String[] curLine = reader.readLine().split(",");
			for (String nodeId : curLine) {
				guesser.generateGuesses(nodeId.trim());
			}
			reader.close();
			
			return;
		}
		
		// Generate edge guesses for nodes in target graph
		int count = 0;
		for (Feature feature : guesser.getSortedTarFeatures()) {
			if (count++ >= numTargets) break;
			guesser.generateGuesses(feature.getName());
		}
	}

}
