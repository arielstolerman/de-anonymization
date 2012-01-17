package reid;

import java.io.*;
import java.util.*;

public abstract class EdgeCollator {

	/********************************************************************************
	 * Abstract Methods
	 ********************************************************************************/
	
	public abstract void collateEdges(String edgeName) throws IOException;

	/********************************************************************************
	 * Instance members
	 ********************************************************************************/

	List<String> guessEdgeList = new ArrayList<String>();
	String baseIn = null;
	String baseOut = null;
	String featureType = null;

	/********************************************************************************
	 *  Constructor
	 ********************************************************************************/

	public EdgeCollator(String baseIn, String baseOut, String featureType) {
		
		this.baseIn = baseIn;
		this.featureType = featureType;
		this.baseOut = baseOut + "-" + featureType;
		String outConf = this.baseOut + "-confusion.txt";
		String outSorted = this.baseOut + "-featmag-sorted.txt";
		String outCDF = this.baseOut + "-cdf.txt";
		String outHist = this.baseOut + "-histogram.txt";
		
		this.initializeTargetNodeList(baseIn, featureType);
	}

	/********************************************************************************
	 * Getter Methods
	 ********************************************************************************/
	
	public List<String> getTargetNodeList() {
		return Collections.unmodifiableList(this.guessEdgeList);
	}

	/********************************************************************************
	 * Instance Methods
	 ********************************************************************************/

	protected void initializeTargetNodeList(String baseIn, String featureType) {
		File dir = new File(baseIn+"-"+featureType);

		for (File file : dir.listFiles()) {
			if(file.isDirectory()) {
				this.guessEdgeList.add(file.getName());
			}
		}
	}
	
	protected List<File> getGuessFiles(String nodeId) {
		List<File> guesses = new ArrayList<File>();
		File dir = new File(this.baseIn + "-" + this.featureType + "/" + nodeId);
		for (File file : dir.listFiles()) {
			if(file.isFile() && file.getName().endsWith(".csv")) {
				guesses.add(file);
			}
		}
		
		return guesses;
	}
}
