/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.data;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class SparseBinaryMatrix<T> extends SparseMatrix<T> {
	private TIntIntMap sparseMap = new TIntIntHashMap();
	
	public SparseBinaryMatrix(int[] dimensions) {
		super(dimensions, false);
	}
	
	public SparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
		super(dimensions, useColumnMajorOrdering);
	}
	
	/**
	 * Sets the object to occupy the specified index.
	 * 
	 * @param index		the index the object will occupy
	 * @param object	the object to be indexed.
	 */
	@Override
	public void set(int index, int value) {
		sparseMap.put(index, value);
	}
	
	/**
	 * Sets the specified object to be indexed at the index
	 * computed from the specified coordinates.
	 * 
	 * @param coordinates	the row major coordinates [outer --> ,...,..., inner]
	 * @param object		the object to be indexed.
	 */
	public void set(int[] coordinates, int value) {
		set(computeIndex(coordinates), value);
	}
	
	/**
	 * Returns the T at the specified index.
	 * 
	 * @param index		the index of the T to return
	 * @return	the T at the specified index.
	 */
	@Override
	public int getIntValue(int index) {
		return sparseMap.get(index);
	}
	
	/**
	 * Returns the T at the index computed from the specified coordinates
	 * @param coordinates	the coordinates from which to retrieve the indexed object
	 * @return	the indexed object
	 */
	public int getIntValue(int[] coordinates) {
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
