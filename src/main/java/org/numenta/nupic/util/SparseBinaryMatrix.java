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

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.lang.reflect.Array;
import java.util.Arrays;

@SuppressWarnings("rawtypes")
public class SparseBinaryMatrix extends SparseMatrix {
    private TIntIntMap sparseMap = new TIntIntHashMap();
    private int[] trueCounts;
    private Object backingArray;
    
    public SparseBinaryMatrix(int[] dimensions) {
        this(dimensions, false);
    }
    
    public SparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        this.backingArray = Array.newInstance(int.class, dimensions);
        this.trueCounts = new int[dimensions[0]];
    }
    
    /**
     * Called during mutation operations to simultaneously set the value
     * on the backing array dynamically.
     * @param val
     * @param coordinates
     */
    private void back(int val, int... coordinates) {
		ArrayUtils.setValue(this.backingArray, val, coordinates);
        //update true counts
		trueCounts[coordinates[0]] = ArrayUtils.aggregateArray(((Object[])this.backingArray)[coordinates[0]]);
	}
    
    /**
     * Returns the slice specified by the passed in coordinates.
     * The array is returned as an object, therefore it is the caller's
     * responsibility to cast the array to the appropriate dimensions.
     * 
     * @param coordinates	the coordinates which specify the returned array
     * @return	the array specified
     * @throws	IllegalArgumentException if the specified coordinates address
     * 			an actual value instead of the array holding it.
     */
     public Object getSlice(int... coordinates) {
		Object slice = backingArray;
		for(int i = 0;i < coordinates.length;i++) {
			slice = Array.get(slice, coordinates[i]);;
		}
		//Ensure return value is of type Array
		if(!slice.getClass().isArray()) {
			throw new IllegalArgumentException(
				"This method only returns the array holding the specified index: " + 
					Arrays.toString(coordinates));
		}
		
		return slice;
	}
    
    /**
     * Fills the specified results array with the result of the 
     * matrix vector multiplication.
     * 
     * @param inputVector		the right side vector
     * @param results			the results array
     */
    public void rightVecSumAtNZ(int[] inputVector, int[] results) {
    	for(int i = 0;i < dimensions[0];i++) {
    		int[] slice = (int[])(dimensions.length > 1 ? getSlice(i) : backingArray);
    		for(int j = 0;j < slice.length;j++) {
    			results[i] += (inputVector[j] * slice[j]);
    		}
    	}
    }
    
    /**
     * Sets the value at the specified index.
     * 
     * @param index     the index the object will occupy
     * @param object    the object to be indexed.
     */
    @Override
    public SparseBinaryMatrix set(int index, int value) {
    	int[] coordinates = computeCoordinates(index);
        return set(value, coordinates);
    }
    
    /**
     * Sets the value to be indexed at the index
     * computed from the specified coordinates.
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     * @param object        the object to be indexed.
     */
    @Override
    public SparseBinaryMatrix set(int value, int... coordinates) {
        sparseMap.put(computeIndex(coordinates), value);
        back(value, coordinates);
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
     * Sets the value at the specified index skipping the automatic
     * truth statistic tallying of the real method.
     * 
     * @param index     the index the object will occupy
     * @param object    the object to be indexed.
     */
    public SparseBinaryMatrix setForTest(int index, int value) {
        sparseMap.put(index, value);         
        return this;
    }
    
    /**
     * Call This for TEST METHODS ONLY
     * Sets the specified values at the specified indexes.
     * 
     * @param indexes   indexes of the values to be set
     * @param values    the values to be indexed.
     * 
     * @return this {@code SparseMatrix} implementation
     */
    public SparseBinaryMatrix set(int[] indexes, int[] values, boolean isTest) { 
        for(int i = 0;i < indexes.length;i++) {
        	if(isTest) setForTest(indexes[i], values[i]);
        	else set(indexes[i], values[i]);
        }
        return this;
    }
    
    /**
     * Returns the count of 1's set on the specified row.
     * @param index
     * @return
     */
    public int getTrueCount(int index) {
        return trueCounts[index];
    }
    
    /**
     * Sets the count of 1's on the specified row.
     * @param index
     * @param count
     */
    public void setTrueCount(int index, int count) {
        this.trueCounts[index] = count;
    }
    
    /**
     * Get the true counts for all outer indexes.
     * @return
     */
    public int[] getTrueCounts() {
        return trueCounts;
    }
    
    /**
     * Clears the true counts prior to a cycle where they're
     * being set
     */
    public void clearStatistics(int row) {
    	int[] slice = (int[])Array.get(backingArray, row);
    	Arrays.fill(slice, 0);
    	trueCounts[row] = 0;
		sparseMap.put(row, 0);
    }
    
    /**
     * Returns an outer array of T values.
     * @return
     */
    @Override
    protected int[] values() {
    	return sparseMap.values();
    }
    
    /**
     * Returns the int value at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    public int getIntValue(int... coordinates) {
    	return sparseMap.get(computeIndex(coordinates));
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
