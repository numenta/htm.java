package org.numenta.nupic.data;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class SparseObjectMatrix<T> extends SparseMatrix<T> {
	private TIntObjectMap<T> sparseMap = new TIntObjectHashMap<T>();
	
	public SparseObjectMatrix(int[] dimensions) {
		super(dimensions, false);
	}
	
	public SparseObjectMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
		super(dimensions, useColumnMajorOrdering);
	}
	
	/**
	 * Sets the object to occupy the specified index.
	 * 
	 * @param index		the index the object will occupy
	 * @param object	the object to be indexed.
	 */
	@Override
	public void set(int index, T object) {
		sparseMap.put(index, object);
	}
	
	/**
	 * Sets the specified object to be indexed at the index
	 * computed from the specified coordinates.
	 * 
	 * @param coordinates	the row major coordinates [outer --> ,...,..., inner]
	 * @param object		the object to be indexed.
	 */
	public void set(int[] coordinates, T object) {
		set(computeIndex(coordinates), object);
	}
	
	/**
	 * Returns the T at the specified index.
	 * 
	 * @param index		the index of the T to return
	 * @return	the T at the specified index.
	 */
	public T get(int index) {
		return sparseMap.get(index);
	}
	
	/**
	 * Returns the T at the index computed from the specified coordinates
	 * @param coordinates	the coordinates from which to retrieve the indexed object
	 * @return	the indexed object
	 */
	public T get(int[] coordinates) {
		return sparseMap.get(computeIndex(coordinates));
	}
	
	/**
	 * Returns a sorted array of occupied indexes.
	 * @return	a sorted array of occupied indexes.
	 */
	public int[] getSparseIndices() {
		return reverse(sparseMap.keys());
	}
	
	
}
