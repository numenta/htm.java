package org.numenta.nupic.encoders;

public interface CoordinateOrder {
	/**
	 * Returns the order for a coordinate.
	 * 
	 * @param coordinate	coordinate array
	 * 
	 * @return	A value in the interval [0, 1), representing the
     *          order of the coordinate
	 */
	public double orderForCoordinate(int[] coordinate);
}
