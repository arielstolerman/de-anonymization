package reid;
import java.io.*;
import java.text.*;
import java.util.*;

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

 * 
 * @author hendersk
 *
 */

public class IDResPostProcess {
	private static final int HASH_DIGITS = 4;
	
	private static List<String> featureNames = new ArrayList<String>();
	private static Map<String, List<Double>> refFeatures = new HashMap<String, List<Double>>();
	private static Map<String, List<Double>> tarFeatures = new HashMap<String, List<Double>>();
	
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
	 *  [3] numExamples - How many example scores to print to stdout
	 *  [4] [numTargets] - optional number of target nodes to test on. Chosen in decreasing
	 *  				   order of feature vector magnitude.
 	 *  [5] [idFile] - optional csv file with one line containing the ids of nodes to be
 	 *  			   analyzed.
 	 * @return writes several files with baseOutputFileName. The most important of these is 
 	 * 			BASE-featureType-cdf.txt, which contains tab-separated lines and describes the cdf of scores.
 	 * 			That is, each line contains a score S (in increasing order) and the number of
 	 * 			nodes with score <= S. Achievable scores start at 1 (perfect guess).
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	
	public static void main(String[] args) throws IOException, ParseException{
		
		String featureType = null;
		int numTargets = Integer.MAX_VALUE;
		String inTarget = null, inReference = null, inFeatNames = null;
		String baseOut = null, baseIn = null;
		String outConf = null, outSorted = null; 
		String outCDF = null, outHist = null;
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
			numConf = new Integer(args[3]).intValue();
			if(args.length > 4) numTargets = new Integer(args[4]).intValue();
			if(args.length > 5) idFile = args[5];
		}
		catch(Exception e) {
			System.err.println("Usage: java IDResPost.jar {local,neighborhood,regional} " + 
			"baseInputFileName baseOutputFileName numExamples [numTargets]");
			return;
		}
		*/

		// TODO hardcoded
		featureType = "local";
		baseIn = IdentityResolution.base;
		baseOut = IdentityResolution.base+"_post_process_nodes";
		numConf = 0;
		
		
		inTarget = baseIn + "-target.txt";
		inReference = baseIn + "-reference.txt";
		inFeatNames = baseIn + "-features.txt";
	
		baseOut = baseOut + "-" + featureType;
		
		outConf = baseOut + "-confusion.txt";
		outSorted = baseOut + "-featmag-sorted.txt";
		
		outCDF = baseOut + "-cdf.txt";
		outHist = baseOut + "-histogram.txt";
		
		BufferedReader reader = new BufferedReader(new FileReader(inFeatNames));
		String curLine = reader.readLine();
		String[] split;
		
		// These features are local
		Set<String> locals = new HashSet<String>();
		locals.add("wn1");
		locals.add("xesu0");
		locals.add("xedu0");
		locals.add("xeu0");
		locals.add("xest0");
		locals.add("xedt0");
		locals.add("xet0");
		
		
		// Decide which columns to use based on featureType
		Map<String, Integer> featureIndices = new HashMap<String, Integer>();
		int index = 0;
		for (String feat : curLine.split(",")) {
			
			// Local features defined above
			if(featureType.equals("local") && 
					locals.contains(feat)) {
				featureNames.add(feat);
				featureIndices.put(feat, index);
			}
			
			// Neighborhood features have no hyphens (non-recursive)
			else if(featureType.equals("neighborhood") &&
					feat.indexOf('-') < 0) {
				featureNames.add(feat);
				featureIndices.put(feat,index);
			}
			
			// Regional features include all columns
			else if(featureType.equals("regional")){
				featureNames.add(feat);
				featureIndices.put(feat,index);
			}
			index++;
		}
		
		// Read target node features
		reader = new BufferedReader(new FileReader(inTarget));
		while((curLine = reader.readLine()) != null) {
			split = curLine.split(",");
			String user = split[0];
			tarFeatures.put(user, new ArrayList<Double>());
			for(String feat : featureNames) {
				tarFeatures.get(user).add(Double.parseDouble(split[1+featureIndices.get(feat)]));
			}
		}
		
		// Read reference node features
		reader = new BufferedReader(new FileReader(inReference));
		while((curLine = reader.readLine()) != null) {
			split = curLine.split(",");
			String user = split[0];
			refFeatures.put(user, new ArrayList<Double>());
			for(String feat : featureNames) {
				refFeatures.get(user).add(Double.parseDouble(split[1+featureIndices.get(feat)]));
			}
		}
		
		
				
		
		int depth = 1;
		for(String s : featureNames) {
			int d = s.split("-").length;
			if (d > depth) depth = d;
		}

		System.out.println(featureNames.size() + " " + 
				featureType + " features");
		System.out.println(depth + " iterations");
		System.out.println();

		
		// Find target nodes that are in reference graph, and sort by feature magnitude.		
		List<Double> tarFeatVals = new ArrayList<Double>();
		for(String id : tarFeatures.keySet()) {
			if(!refFeatures.containsKey(id)) continue;
			tarFeatVals.add(magnitude(tarFeatures.get(id)));
		}
		Collections.sort(tarFeatVals);
		
		// Compute the number of targets to analyze, and the minimum feature vector
		// magnitude to consider (enables computational shortcut).
		int numTars = tarFeatVals.size();
		if(numTargets > numTars) numTargets = numTars;
		if(numTargets < 1) numTargets = 1;
		double threshold = tarFeatVals.get(numTars - numTargets);

		if(idFile == null) {
			System.out.println("Min node vector magnitude: " + Math.sqrt(tarFeatVals.get(0)));
			System.out.println("Max node vector magnitude: " + Math.sqrt(tarFeatVals.get(numTars-1)));
			System.out.println("Threshold for analyzing node: " + Math.sqrt(threshold));
		}
		
		Set<String> samples = new HashSet<String>();
		if(idFile != null) {
			reader = new BufferedReader(new FileReader(idFile));
			curLine = reader.readLine();
			split = curLine.split(",");
			for(String name : split) {
				samples.add(name);
			}
		}
		
		System.out.println(samples.size() + " Samples Found");
		
		// Prune the target feature map to include only nodes that we want to analyze.
		Set<String> tarKeys = new HashSet<String>();
		tarKeys.addAll(tarFeatures.keySet());
		for(String k : tarKeys) {
			if(idFile != null) {
				if(!samples.contains(k)) {
					tarFeatures.remove(k);
				}
			}
			else {
				if(!refFeatures.containsKey(k) || 
						magnitude(tarFeatures.get(k)) < threshold) {
					tarFeatures.remove(k);
				}

			}
		}

		

		System.out.println("Number of nodes analyzed: " + tarFeatures.size());
		System.out.println();
		
		// How many nodes had score S? Map from S -> count of nodes
		Map<Integer, Integer> featurePDF = new HashMap<Integer, Integer>();
		featurePDF.put(0, 0);
		
		// Map from approximate magnitudes to nodes with that approximate magnitude
		Map<Integer, Set<String>> sortedRadiiP = new HashMap<Integer, Set<String>>();		
		for(String nodeID : refFeatures.keySet()) {
			int k = hashDouble(magnitude(refFeatures.get(nodeID)), HASH_DIGITS);
			if(!sortedRadiiP.containsKey(k)) sortedRadiiP.put(k, new HashSet<String>());
			sortedRadiiP.get(k).add(nodeID);
		}
		
		// Compute the score for each target node, put it in the PDF.
		Map<String, Integer> featureScores = new HashMap<String, Integer>();
		for(String nodeID : tarFeatures.keySet()) {
			int d = euclidRank(nodeID, sortedRadiiP);
			if(d >= 0) {				
				if(!featurePDF.containsKey(d)) featurePDF.put(d, 0);
				featurePDF.put(d, featurePDF.get(d) + 1);
				featureScores.put(nodeID, d);
			}
		}
		
		System.out.println("Comparisons: used " + 
				humanReadable(usedComparisons) + 
				" of " + 
				humanReadable(usedComparisons + savedComparisons) + 
				" (" +	humanReadable(savedComparisons) + " saved - " + 
				percent(((double)savedComparisons)/
						(usedComparisons+savedComparisons)) + 
				"%)\n");
		
		
		System.out.println("Differences: used " + 
				humanReadable(usedDiffs) + 
				" of " + 
				humanReadable(usedDiffs + savedDiffs) + 
				" (" +	humanReadable(savedDiffs) + " saved - " + 
				percent(((double)savedDiffs)/
						(usedDiffs+savedDiffs)) + 
				"%)\n");
		
		// Convert PDF to CDF for easier comparison of approaches
		List<Integer> pdfKeys = new ArrayList<Integer>();
		List<Integer> featureCDF = new ArrayList<Integer>();
		
		for(int k : featurePDF.keySet()) pdfKeys.add(k);
		Collections.sort(pdfKeys);
		
		
		
		int tot = 0;
		for(int k : pdfKeys) {
			tot += featurePDF.get(k);
			featureCDF.add(tot);
		}
				
		// Write a file with "confusion" lists for top few nodes (by score)
		PrintStream out = new PrintStream(outConf);
		Map<String, Integer> confTest = new HashMap<String, Integer>();
		String[] confNames, loserNames;
		double[] confScores, loserDists;
		int[] confOrder, loserOrder;
		Map<String, Double> losers;
		
		
		index = 0;
		int j = 0;
		while(index < featureCDF.size() && featureCDF.get(index) < numConf) index++;
		if(index == featureCDF.size()) index--;
		index = pdfKeys.get(index);
		System.out.println("Confusion thresh = " + index);
		for(String nodeID : featureScores.keySet()) {
			if(featureScores.get(nodeID) <= index) {
				confTest.put(nodeID, featureScores.get(nodeID));
			}
		}
		
		confNames = new String[confTest.size()];
		confScores = new double[confTest.size()];
		j = 0;
		for(String key : confTest.keySet()) {
			confNames[j] = key;
			confScores[j++] = confTest.get(key);
		}
		confOrder = SortUtils.indexSort(confScores);
		
		for(int i = 0; i < confOrder.length; i++) {
			String user = confNames[confOrder[i]];
			System.out.println("Confusion: " + user + 
					" ( " + confTest.get(user) + " )");
			
			losers = losers(user, sortedRadiiP);
			loserNames = new String[losers.size()];
			loserDists = new double[losers.size()];
			j=0;
			for(String key : losers.keySet()) {
				loserNames[j] = key;
				loserDists[j++] = losers.get(key);
			}
			
			loserOrder = SortUtils.indexSort(loserDists);
			
			out.println(user + " " + confTest.get(user));
			
			for(j = 0; j < loserOrder.length; j++) {
				out.println((j+1) + "\t" + loserNames[loserOrder[j]] + 
						"\t" + loserDists[loserOrder[j]]);
			}
			
			out.println();
		}
		out.close();
		System.out.println();
		
		
		// Write a file with each node, its feature magnitude, and its score. Sorted
		// by increasing feature magnitude.
		out = new PrintStream(outSorted);
		int[] magOrder;
		String[] targetNames;
		double[] targetMags;

		targetNames = new String[tarFeatures.size()];
		targetMags = new double[tarFeatures.size()];
		
		j = 0;
		for(String test : tarFeatures.keySet()) {
			targetNames[j] = test;
			targetMags[j++] = magnitude(tarFeatures.get(test));
		}
		magOrder = SortUtils.indexSort(targetMags);
		
		for(int i : magOrder) {
			String user = targetNames[i];
			double mag = targetMags[i];
			int rank = featureScores.containsKey(user)?featureScores.get(user):0;
			out.println(user + "\t" + mag + "\t" + rank);
		}
		out.close();
		
		// Write CDF to file.
		out = new PrintStream(outCDF);
		for(int i = 0; i < featureCDF.size(); i++) {
			out.println(pdfKeys.get(i) + "\t" + featureCDF.get(i));
		}
		out.println(refFeatures.size() + "\t" + featureCDF.get(featureCDF.size()-1));
		out.close();
		
		
		// Write some histogram information about the PDF.
		out = new PrintStream(outHist);
		double expected = 0, count = 0;
		for(int key : pdfKeys) {
			expected += key*featurePDF.get(key);
			count += featurePDF.get(key);
		}
		if(count > 0) expected /= count;
		
		String[] histNames = new String[]{
				"0% - 10%", "10% - 20%",
				"20% - 30%", "30% - 40%", 
				"40% - 50%", "50% - 60%", "60% - 70%",
				"70% - 80%", "80% - 90%", "90% - 100%"};
		List<Double> histIndex = new ArrayList<Double>();
		for(int i = 1; i < 11; i++) {
			histIndex.add(i*0.1);
		}
		long[] histogram = new long[10];
		
		for(int score : pdfKeys) {
			int scoreCount = featurePDF.get(score);
			for(int i = 0; i < 10; i++) {
				if(score <= histIndex.get(i)*refFeatures.size()) {
					histogram[i]+=scoreCount;
					break;
				}
			}
		}
		
		String[] cdfBinNames = new String[]{
				"0% - 1%", "0% - 5%", "0% - 10%", 
				"0% - 50%", "0% - 100%"};
		List<Double> cdfBinIndex = new ArrayList<Double>();
		cdfBinIndex.add(0.01);
		cdfBinIndex.add(0.05);
		cdfBinIndex.add(0.1);
		cdfBinIndex.add(0.5);
		cdfBinIndex.add(1.0);
		long[] cdfBinVals = new long[5];
		
		for(int score : pdfKeys) {
			int scoreCount = featurePDF.get(score);
			for(int i = 0; i < 5; i++) {
				if(score <= cdfBinIndex.get(i)*refFeatures.size()) {
					cdfBinVals[i]+=scoreCount;
				}
			}
		}
		
		out.println("Expected Score");
		out.println("\tBaseline\t"+featureType);
		out.println("\t" + refFeatures.size()/2.0 + "\t" + (int)Math.ceil(expected));
		out.println();
		
		out.println("Histogram");
		out.println("\tBaseline\t"+featureType);
		for(int i = 0; i < 10; i++) {
			out.println(histNames[i] + "\t0.1\t" + 1.0*histogram[i]/count);
		}
		out.println();
		
		out.println("Cumulative Histogram");
		out.println("\tBaseline\t"+featureType);
		for(int i = 0; i < 5; i++){
			out.println(cdfBinNames[i] + "\t" 
					+ cdfBinIndex.get(i) + "\t" 
					+ 1.0*cdfBinVals[i]/count);
		}
		
		
	}
	
	// Bookkeeping to see how good our optimizations are.
	private static long usedComparisons = 0;
	private static long savedComparisons = 0;
	
	/**
	 * Computes score for a given node, based on magnitudes of reference nodes.
	 * 
	 * @param nodeID - the node to score
	 * @param sortedRadii - map from approximate magnitudes to reference node counts
	 * @return - score of nodeID
	 */
	private static int euclidRank(String nodeID, Map<Integer,Set<String>> sortedRadii) {
		
		// Check for node being in reference graph.
		if(!refFeatures.containsKey(nodeID)) return -1;
		
		
		// Put node's feature vectors in arrays
		int cnt = 0;
		double[] p1 = new double[featureNames.size()];
		double[] p2 = new double[featureNames.size()];
		for(int i = 0; i < featureNames.size(); i++) {
			p1[i] = tarFeatures.get(nodeID).get(i);
			p2[i] = refFeatures.get(nodeID).get(i);
		}
		
		
		// Length of target feature vector
		double targetRad = Math.sqrt(magnitude(tarFeatures.get(nodeID)));
		
		// Distance between target and reference feature vectors. Any reference nodes
		// closer than this will increase (worsen) the score for this node.
		double dist1 = euclid(p1, p2);
		
		double dist2;
		double sqrtDist = Math.sqrt(dist1);
		int ties = 0;

		// We will analyze features in decreasing order of target magnitude
		int[] index = SortUtils.reverse(SortUtils.indexSort(p1));
		
		// Iterate over reference node bins (approximate magnitudes).
		for(int k : sortedRadii.keySet()) {
			
			// approximate magnitude of this bin
			double refRad = Math.sqrt(unhashDouble(k, HASH_DIGITS));
			
			// No need to check nodes if they are not possibly close enough,
			// based on their feature magnitude and the target distance.
			if(refRad + sqrtDist < targetRad || 
					targetRad + sqrtDist < refRad) {
				savedComparisons += sortedRadii.get(k).size();
				continue;
			}
			
			// We need to check these nodes explicitly.
			usedComparisons += sortedRadii.get(k).size();
			for(String node2 : sortedRadii.get(k)) {
				for(int i = 0; i < featureNames.size(); i++) {
					p2[i] = refFeatures.get(node2).get(i);
				}
				
				dist2 = euclid(p1, p2, dist1, index);
				if(dist2 <= dist1) {
					cnt++;
				}
				if(dist2 == dist1) {
					ties++;
				}
			}
		}
		return cnt;
	}
	
	/**
	 * Computes the "losers" for a given node. Same algorithm as euclidRank.
	 * 
	 * @param nodeID
	 * @param sortedRadii
	 * @return
	 */
	private static Map<String, Double> losers(String nodeID, Map<Integer,Set<String>> sortedRadii) {
		Map<String, Double> ret = new HashMap<String, Double>();
		
		int cnt = 0;
		double[] p1 = new double[featureNames.size()];
		double[] p2 = new double[featureNames.size()];
		for(int i = 0; i < featureNames.size(); i++) {
			p1[i] = tarFeatures.get(nodeID).get(i);
			p2[i] = refFeatures.get(nodeID).get(i);
		}
				
		
		double targetRad = Math.sqrt(magnitude(tarFeatures.get(nodeID)));
		double dist1 = euclid(p1, p2), dist2;
		double sqrtDist = Math.sqrt(dist1);
		int ties = 0;

		int[] index = SortUtils.reverse(SortUtils.indexSort(p1));
		
		for(int k : sortedRadii.keySet()) {
			double refRad = Math.sqrt(unhashDouble(k, HASH_DIGITS));
			if(refRad + sqrtDist < targetRad || 
					targetRad + sqrtDist < refRad) {
				savedComparisons += sortedRadii.get(k).size();
				continue;
			}
			usedComparisons += sortedRadii.get(k).size();
			for(String node2 : sortedRadii.get(k)) {
				for(int i = 0; i < featureNames.size(); i++) {
					p2[i] = refFeatures.get(node2).get(i);
				}
				dist2 = euclid(p1, p2, dist1, index);
				if(dist2 <= dist1) {
					cnt++;
					ret.put(node2, dist2);
				}
				if(dist2 == dist1) {
					ties++;
				}
			}
		}
		return ret;
	}
	
	
	/**
	 * Computes feature magnitude squared.
	 * 
	 * @param values - feature vector
	 * @return
	 */
	private static double magnitude(List<Double> values) {
		double tot = 0.0;
		for (double v : values) tot += v*v;
		return tot;
	}
	
	/**
	 * Hashes a double into an integer, losing some precision.
	 * @param d - the double
	 * @param digits - how many fractional digits to retain
	 * @return the integer
	 */
	private static int hashDouble(double d, int digits) {
		d *= Math.pow(10, digits);
		return (int) d;
	}
	
	/**
	 * Turns an integer into the (approximate) double that generated it.
	 * @param k - the integer
	 * @param digits - how many fractional digits were retained
	 * @return the approximate double
	 */
	private static double unhashDouble(int k, int digits) {
		return ((double)k)/Math.pow(10, digits);
	}
	
	// Makes big numbers readable
	private static String humanReadable(long num) {
		if(num < 1000) return ""+ num;
		if(num < 1000000) return "" + num/1000 + "K";
		if(num < 1000000000) return "" + num/1000000 + "M";
		return num/1000000000 + "G";
	}
	
	// Turns doubles into integer percentages
	private static int percent(double num) {
		return (int)(num*100);
	}
	
	// Computes the square of the distance between vectors p1 and p2
	private static double euclid(double[] p1, double[] p2) { 
		double diff, d = 0.0;
		for (int i = 0; i < p1.length; i++) {
			diff = p1[i]-p2[i];
			d += diff*diff;
		}
		return d;
	}
	
	// Bookkeeping to see how our optimization is doing
	private static long usedDiffs = 0;
	private static long savedDiffs = 0;
	
	/**
	 * Computes the square of the distance between vectors p1 and p2, in an optimized way.
	 * If we know the maximum distance we are concerned with, we can bail out once we get
	 * past that distance. We use the "index" argument to start with the biggest features
	 * first, further improving runtime.
	 * 
	 * @param p1
	 * @param p2
	 * @param max
	 * @param index
	 * @return
	 */
	private static double euclid(double[] p1, double[] p2, double max, int[] index) { 
		double diff, d = 0.0;
		for (int i = 0; i < index.length; i++) {
			usedDiffs++;
			diff = p1[index[i]]-p2[index[i]];
			d += diff*diff;
			
			// We can abort here because the vectors are far enough away
			if(d > max) {
				savedDiffs += index.length  - i - 1;
				return d;
			}
		}
		return d;
	}
	
	
}
