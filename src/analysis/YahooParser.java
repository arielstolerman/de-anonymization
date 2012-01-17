package analysis;

import java.io.*;
import java.util.*;

import reid.*;

public class YahooParser {
	
	public static String baseDir = "./data/yahoo/";
	public static String prefix = "yahoo_";
	public static int maxDist = 0;
	public static double binSize = 0.5;
	
	public static void usageExit() {
		System.out.println("Usage:");
		System.out.println("java -jar reid_yahoo.jar <max-dist> <bin-size> <ref-from-index> <ref-to-index>");
		System.out.println("Example:");
		System.out.println("java -jar reid_yahoo.jar 2 0.5 1 27");
		System.out.println("(will run ReFex on d00 as target and d01,...,d27 as reference)");
		System.exit(1);
	}
	
	/**
	 * YahooParser entry point.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		// create data
		//parseAllYahoo();
		
		int refStart = 0;
		int refEnd = 0;
		
		if (args.length != 4) {
			System.out.println("Wrong number of arguments.");
			usageExit();
		}
		
		try {
			maxDist = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println("maxDist must be an integer.");
			usageExit();
		}
		
		try {
			binSize = Double.parseDouble(args[1]);
		} catch (NumberFormatException e) {
			System.out.println("binSize must be a double.");
			usageExit();
		}
		
		try {
			refStart = Integer.parseInt(args[2]);
			if (refStart < 1 || refStart > 27)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			System.out.println("ref-from-index must be an integer between 1 and 27.");
			usageExit();
		}
		
		try {
			refEnd = Integer.parseInt(args[3]);
			if (refEnd < 1 || refEnd > 27 || refEnd < refStart)
				throw new NumberFormatException();
		} catch (NumberFormatException e) {
			System.out.println("ref-to-index must be an integer between 1 and 27 and bigger than ref-from-index.");
			usageExit();
		}
		
		System.out.println("Running ReFex with maxDist = "+maxDist+", binSize = "+binSize+" with day 0 as target and days "+refStart+" - "+refEnd+" as reference.");
		
		// run ReFex
		int tar = 0;
		for (int ref = refStart; ref <=refEnd; ref++) {
			System.out.println("Yahoo IdentityResolution on target day "+tar+", reference day "+ref);
			System.out.println("====================================================================");
			runYahooIdRes(tar,ref);
		}
	}
	
	// =================================================================================================
	
	public static void runYahooIdRes(int tar, int ref) {
		String tarDay = "d"+(tar < 10 ? "0" : "")+tar;
		String refDay = "d"+(ref < 10 ? "0" : "")+ref;
		String base = baseDir+"yahoo-"+tarDay+"-"+refDay;
		
		String tarPath = base+"-target-graph.txt";
		String refPath = base+"-reference-graph.txt";
		
		// make copies of the graphs
		System.out.println("copying target graph to "+tarPath+"...");
		try {
			copy(new File(baseDir+prefix+tarDay+".txt"), new File(tarPath));
		} catch (IOException e) {
			System.err.println("Failed copying "+baseDir+prefix+tarDay+".txt to "+tarPath);
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("done!");
		System.out.println("copying reference graph to "+refPath+"...");
		try {
			copy(new File(baseDir+prefix+tarDay+".txt"), new File(refPath));
		} catch (IOException e) {
			System.err.println("Failed copying "+baseDir+prefix+tarDay+".txt to "+refPath);
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("done!");
		System.out.println();
		System.out.println("running ReFex...");
		try {
			IdentityResolution.resetStaticVars();
			IdentityResolution.main(new String[]{tarPath,refPath,""+maxDist,""+binSize,base});
		} catch (Exception e) {
			System.err.println("Failed running re-id on target "+tarDay+", reference "+refDay);
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("done!");
	}
	
	/**
	 * Main method to parse all yahoo data
	 * @throws Exception
	 */
	public static void parseAllYahoo() throws Exception {
		File[] yahoo_raw = new File("./data/yahoo_raw").listFiles();
		int lines;
		for (File f: yahoo_raw) {
			System.out.println("parsing file "+f.getName()+"...");
			lines = parseFile(f);
			System.out.println("done! wrote "+lines+" lines");
		}
	}
	
	/**
	 * Extracts the day string from the given yahoo file path.
	 * @param path
	 * @return
	 */
	public static String getDay(String path) {
		int n = path.length();
		return path.substring(n-7,n-4);
	}
	
	/**
	 * Parses the raw yahoo data into CSV format graph like in henderson-kdd2011.pdf. 
	 * @param file
	 * @throws Exception
	 */
	public static int parseFile(File file) throws Exception {
		Map<String,Map<String,Integer>> map = new HashMap<String,Map<String,Integer>>();
		Scanner scan = new Scanner(new FileReader(file));
		
		// create map
		String[] line;
		String from, to;
		Map<String,Integer> nodeMap;
		Integer i;
		while (scan.hasNext()) {
			line = scan.nextLine().split(" ");
			from = line[2];
			to = line[4];
			nodeMap = map.get(from);
			if (nodeMap == null) {
				nodeMap = new HashMap<String,Integer>();
				map.put(from, nodeMap);
			}
			i = nodeMap.get(to);
			if (i == null) {
				nodeMap.put(to, 1);
			} else {
				nodeMap.put(to,i+1);
			}
		}
		
		// write results in sorted order
		int lines = 0;
		BufferedWriter bw = new BufferedWriter(new FileWriter(baseDir+prefix+getDay(file.getName())+".txt"));
		List<String> sortedSources = new ArrayList<String>(map.keySet());
		List<String> sortedDests;
		Collections.sort(sortedSources);
		for (String source: sortedSources) {
			sortedDests = new ArrayList<String>(map.get(source).keySet());
			Collections.sort(sortedDests);
			for (String dest: sortedDests) {
				bw.write(source+","+dest+","+map.get(source).get(dest)+"\n");
				lines++;
			}
		}
		return lines;
	}

	/**
	 *  Copies src file to dst file. If the dst file does not exist, it is created.
	 * @param src
	 * @param dst
	 * @throws IOException
	 */
	public static void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}
}
