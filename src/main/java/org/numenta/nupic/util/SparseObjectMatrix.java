/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.util;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Arrays;

/**
 * Allows storage of array data in sparse form, meaning that the indexes
 * of the data stored are maintained while empty indexes are not. This allows
 * savings in memory and computational efficiency because iterative algorithms
 * need only query indexes containing valid data.
 * 
 * @author David Ray
 *
 * @param <T>
 */
public class SparseObjectMatrix<T> extends SparseMatrix<T> {
    private TIntObjectMap<T> sparseMap = new TIntObjectHashMap<T>();
    
    /**
     * Constructs a new {@code SparseObjectMatrix}
     * @param dimensions	the dimensions of this array
     */
    public SparseObjectMatrix(int[] dimensions) {
        super(dimensions, false);
    }
    
    /**
     * Constructs a new {@code SparseObjectMatrix}
     * @param dimensions					the dimensions of this array
     * @param useColumnMajorOrdering		where inner index increments most frequently
     */
    public SparseObjectMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
    }
    
    /**
     * Sets the object to occupy the specified index.
     * 
     * @param index     the index the object will occupy
     * @param object    the object to be indexed.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends SparseMatrix<T>> S set(int index, T object) {
        sparseMap.put(index, (T)object);
        return (S)this;
    }
    
    /**
     * Sets the specified object to be indexed at the index
     * computed from the specified coordinates.
     * @param object        the object to be indexed.
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends SparseMatrix<T>> S set(int[] coordinates, T object) {
        set(computeIndex(coordinates), object);
        return (S)this;
    }
    
    /**
     * Returns an outer array of T values.
     * @return
     */
    @SuppressWarnings("unchecked")
	@Override
    protected T[] values() {
    	return (T[])sparseMap.values();
    }
    
    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    @Override
    public T getObject(int index) {
        return sparseMap.get(index);
    }
    
    /**
     * Returns the T at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    @Override
    public T get(int... coordinates) {
        return sparseMap.get(computeIndex(coordinates));
    }
    
    /**
     * Returns a sorted array of occupied indexes.
     * @return  a sorted array of occupied indexes.
     */
    @Override
    public int[] getSparseIndices() {
        return reverse(sparseMap.keys());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
    	return Arrays.toString(dimensions);
    }
}
