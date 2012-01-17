package reid;

/**
 * Class to hold ReFeX feature information.
 * 
 * @author lever1
 *
 */

import java.util.*;

public class ReFeXFeature implements Feature{
	protected String name = null;
	protected List<Double> features = null;
	protected Double magnitude = null;

	public ReFeXFeature() {}
	
	public ReFeXFeature(String name) {
		this.name = name;
	}
	
	public ReFeXFeature(String name, double magnitude) {
		this.name = name;
		this.magnitude = magnitude;
	}
	
	public ReFeXFeature(String name, List<Double> features) {
		this.name = name;
		this.features = features;
		
		// Calculate feature magnitude
		this.magnitude = 0.0;
		for (double v : this.features) this.magnitude += v*v;
	}
	
	/********************************************************************************
	 * Getter Methods
	 ********************************************************************************/
		
	public String getName() {
		return this.name;
	}
	
	public List<Double> getFeatures() {
		return this.features;
	}
	
	public double getMagnitude() {
		return this.magnitude;
	}

	/********************************************************************************
	 * Setter Methods
	 ********************************************************************************/
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void addFeature(List<Double> feature) {
		if (this.features == null) 
			this.features = feature;
		else 
			this.features.addAll(feature);
	}
	
	public void addFeature(double feature) {
		if (this.features == null)
			this.features = new ArrayList<Double>();
		this.features.add(feature);
	}
	
	public void setMagnitude(double magnitude) {
		this.magnitude = magnitude;
	}
	
	/********************************************************************************
	 * Implement Comparable methods
	 ********************************************************************************/

	public double getDistance(Feature feature) {
		Iterator<Double> iter1 = this.features.iterator();
		Iterator<Double> iter2 = feature.getFeatures().iterator();
	
		double tot = 0.0, diff = 0.0;
		while(iter1.hasNext() && iter2.hasNext()) {
			diff = iter1.next() - iter2.next();
			tot += diff*diff;
		}
		
		return tot;
	}
	
	/********************************************************************************
	 * Implement Comparable methods
	 ********************************************************************************/
	
	// Sort largest magnitude to smallest
	public int compareTo(Feature o) {
		return (int)(o.getMagnitude() - this.getMagnitude());
	}
		
}
