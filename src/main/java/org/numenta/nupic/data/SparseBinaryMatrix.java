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

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.util.Arrays;

@SuppressWarnings("rawtypes")
public class SparseBinaryMatrix extends SparseMatrix {
    private TIntIntMap sparseMap = new TIntIntHashMap();
    
    public SparseBinaryMatrix(int[] dimensions) {
        super(dimensions, false);
    }
    
    public SparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
    }
    
    /**
     * Sets the value at the specified index.
     * 
     * @param index     the index the object will occupy
     * @param object    the object to be indexed.
     */
    @Override
    public SparseBinaryMatrix set(int index, int value) {
        sparseMap.put(index, value);
        return this;
    }
    
    /**
     * Sets the value to be indexed at the index
     * computed from the specified coordinates.
     * 
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     * @param object        the object to be indexed.
     */
    @Override
    public SparseBinaryMatrix set(int[] coordinates, int value) {
        set(computeIndex(coordinates), value);
        return this;
    }
    
    /**
     * Sets the specified values at the specified indexes.
     * 
     * @param indexes   indexes of the values to be set
     * @param values    the values to be indexed.
     * 
     * @return this {@code SparseMatrix} implementation
     */
    public SparseBinaryMatrix set(int[] indexes, int[] values) { 
        for(int i = 0;i < indexes.length;i++) {
            set(indexes[i], values[i]);
        }
        return this;
    }
    
    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    @Override
    public int getIntValue(int index) {
        return sparseMap.get(index);
    }
    
    /**
     * Returns the T at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    @Override
    public int getIntValue(int[] coordinates) {
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
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing
     * the inputMatrix with the contents of this matrix; returning this matrix
     * as the result.
     * 
     * @param inputMatrix   the matrix containing the "on" bits to or
     * @return  this matrix
     */
    public SparseBinaryMatrix or(SparseBinaryMatrix inputMatrix) {
        int[] mask = inputMatrix.getSparseIndices();
        int[] ones = new int[mask.length];
        Arrays.fill(ones, 1);
        return set(mask, ones);
    }
    
    /**
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing
     * the sparse list with the contents of this matrix; returning this matrix
     * as the result.
     * 
     * @param onBitIndexes  the matrix containing the "on" bits to or
     * @return  this matrix
     */
    public SparseBinaryMatrix or(TIntCollection onBitIndexes) {
        int[] ones = new int[onBitIndexes.size()];
        Arrays.fill(ones, 1);
        return set(onBitIndexes.toArray(), ones);
    }
    
    /**
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing
     * the sparse array with the contents of this matrix; returning this matrix
     * as the result.
     * 
     * @param onBitIndexes  the int array containing the "on" bits to or
     * @return  this matrix
     */
    public SparseBinaryMatrix or(int[] onBitIndexes) {
        int[] ones = new int[onBitIndexes.length];
        Arrays.fill(ones, 1);
        return set(onBitIndexes, ones);
    }
    
    /**
     * Returns true if the on bits of the specified matrix are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean all(SparseBinaryMatrix matrix) {
        return sparseMap.keySet().containsAll(matrix.sparseMap.keys());
    }
    
    /**
     * Returns true if the on bits of the specified list are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean all(TIntCollection onBits) {
        return sparseMap.keySet().containsAll(onBits);
    }
    
    /**
     * Returns true if the on bits of the specified array are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean all(int[] onBits) {
        return sparseMap.keySet().containsAll(onBits);
    }
    
    /**
     * Returns true if any of the on bits of the specified matrix are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean any(SparseBinaryMatrix matrix) {
        for(int i : matrix.sparseMap.keys()) {
            if(sparseMap.containsKey(i)) return true;
        }
        return false;
    }
    
    /**
     * Returns true if any of the on bit indexes of the specified collection are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean any(TIntList onBits) {
        for(TIntIterator i = onBits.iterator();i.hasNext();) {
            if(sparseMap.containsKey(i.next())) return true;
        }
        return false;
    }
    
    /**
     * Returns true if any of the on bit indexes of the specified matrix are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean any(int[] onBits) {
        for(int i : onBits) {
            if(sparseMap.containsKey(i)) return true;
        }
        return false;
    }
}
