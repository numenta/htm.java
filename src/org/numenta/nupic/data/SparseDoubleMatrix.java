package org.numenta.nupic.data;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class SparseDoubleMatrix<T> extends SparseMatrix<T> {
	private TIntDoubleMap sparseMap = new TIntDoubleHashMap();
	
	public SparseDoubleMatrix(int[] dimensions) {
		super(dimensions, false);
	}
	
	public SparseDoubleMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
		super(dimensions, useColumnMajorOrdering);
	}
	
	/**
	 * Sets the specified object to be indexed at the index
	 * computed from the specified coordinates.
	 * 
	 * @param coordinates	the row major coordinates [outer --> ,...,..., inner]
	 * @param object		the object to be indexed.
	 */
	@Override
	public void set(int[] coordinates, double value) {
		set(computeIndex(coordinates), value);
	}
	
	/**
	 * Returns the T at the specified index.
	 * 
	 * @param index		the index of the T to return
	 * @return	the T at the specified index.
	 */
	@Override
	public double getDoubleValue(int index) {
		return sparseMap.get(index);
	}
	
	/**
	 * Returns the T at the index computed from the specified coordinates
	 * @param coordinates	the coordinates from which to retrieve the indexed object
	 * @return	the indexed object
	 */
	@Override
	public double getDoubleValue(int[] coordinates) {
		return sparseMap.get(computeIndex(coordinates));
	}
	
	/**
	 * Returns a sorted array of occupied indexes.
	 * @return	a sorted array of occupied indexes.
	 */
	@Override
	public int[] getSparseIndices() {
		return reverse(sparseMap.keys());
	}
	
}
