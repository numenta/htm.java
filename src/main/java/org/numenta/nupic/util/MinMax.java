package org.numenta.nupic.util;

/**
 * Holds two values, a min and a max. Can later be developed to
 * employ operations on those values (i.e. distance etc.)
 * 
 * @author David Ray
 */
public class MinMax {
	private double min;
	private double max;
	
	/**
	 * Constructs a new {@code MinMax} instance
	 */
	public MinMax(){}
	/**
	 * Constructs a new {@code MinMax} instance
	 * 
	 * @param min	the minimum or lower bound
	 * @param max	the maximum or upper bound
	 */
	public MinMax(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Returns the configured min value
	 */
	public double min() {
		return min;
	}
	
	/**
	 * Returns the configured max value
	 */
	public double max() {
		return max;
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append(min).
			append(", ").append(max).toString();
	}
}
