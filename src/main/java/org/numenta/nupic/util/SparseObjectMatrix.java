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

import java.util.Arrays;

import org.numenta.nupic.model.Persistable;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

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
public class SparseObjectMatrix<T> extends AbstractSparseMatrix<T> implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
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
    @Override
    public SparseObjectMatrix<T> set(int index, T object) {
        sparseMap.put(index, (T)object);
        return this;
    }

    /**
     * Sets the specified object to be indexed at the index
     * computed from the specified coordinates.
     * @param object        the object to be indexed.
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     */
    @Override
    public SparseObjectMatrix<T> set(int[] coordinates, T object) {
        set(computeIndex(coordinates), object);
        return this;
    }

    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    @Override
    public T getObject(int index) {
        return get(index);
    }

    /**
     * Returns the T at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    @Override
    public T get(int... coordinates) {
        return get(computeIndex(coordinates));
    }

    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    @Override
    public T get(int index) {
        return this.sparseMap.get(index);
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
        return Arrays.toString(getDimensions());
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((sparseMap == null) ? 0 : sparseMap.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(!super.equals(obj))
            return false;
        if(getClass() != obj.getClass())
            return false;
        SparseObjectMatrix other = (SparseObjectMatrix)obj;
        if(sparseMap == null) {
            if(other.sparseMap != null)
                return false;
        } else if(!sparseMap.equals(other.sparseMap))
            return false;
        return true;
    }

}
