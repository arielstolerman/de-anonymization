package reid;

public abstract class VertexScore implements Comparable<VertexScore> {

	/********************************************************************************
	 *  Instance Members
	 ********************************************************************************/

	protected String name;
	protected double score = 0.0;
	protected int count = 0;
	
	/********************************************************************************
	 *  Constructor
	 ********************************************************************************/

	public VertexScore(String name) {
		this.name = name;
	}

	/********************************************************************************
	 *  Getter Methods
	 ********************************************************************************/
	
	public String getName() {
		return this.name;
	}
	
	/********************************************************************************
	 *  Abstract Methods
	 ********************************************************************************/

	public abstract double getScore();

	/********************************************************************************
	 *  Instance Methods
	 ********************************************************************************/

	public void addTo(double d) {
		this.score += d;
		this.count++;
	}
	
	public int compareTo(VertexScore o) {
		if ((this.getScore() - o.getScore()) < 0.0000001)	return 0;
		return (int)(this.getScore() - o.getScore());
	}
}
