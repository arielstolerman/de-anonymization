package reid_v01;

/**
 * Provide a generic interface for defining features.
 * 
 * @author lever1
 *
 */

import java.util.*;

public interface Feature extends Comparable<Feature> {

		// Getter methods
	
		public String getName();
		public List<Double> getFeatures();
		public double getMagnitude();
		
		// Setter methods
		
		public void setName(String name);
		public void addFeature(List<Double> feature);
		public void addFeature(double feature);
		public void setMagnitude(double feature);
		
		// Instance methods
		
		public double getDistance(Feature feature);
}
