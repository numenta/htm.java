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

import java.lang.reflect.Array;
import java.util.Arrays;

public class SparseBinaryMatrix extends SparseBinaryMatrixSupport {
    private Object backingArray;
    
    public SparseBinaryMatrix(int[] dimensions) {
        this(dimensions, false);
    }
    
    public SparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        this.backingArray = Array.newInstance(int.class, dimensions);
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
		setTrueCount(coordinates[0], ArrayUtils.aggregateArray(((Object[])this.backingArray)[coordinates[0]]));
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
     @Override
     public Object getSlice(int... coordinates) {
		Object slice = backingArray;
		for(int i = 0;i < coordinates.length;i++) {
			slice = Array.get(slice, coordinates[i]);
		}
		//Ensure return value is of type Array
		if(!slice.getClass().isArray()) {
			sliceError(coordinates);
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
    public SparseBinaryMatrixSupport set(int index, int value) {
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
    public SparseBinaryMatrixSupport set(int value, int... coordinates) {
    	super.set(value, coordinates);
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
    public SparseBinaryMatrixSupport set(int[] indexes, int[] values) { 
        for(int i = 0;i < indexes.length;i++) {
            set(indexes[i], values[i]);
        }
        return this;
    }
   
    /**
     * Clears the true counts prior to a cycle where they're
     * being set
     */
    public void clearStatistics(int row) {
    	super.clearStatistics(row);
    	int[] slice = (int[])Array.get(backingArray, row);
    	Arrays.fill(slice, 0);

    
    }

	/* (non-Javadoc)
	 * @see org.numenta.nupic.util.FlatMatrix#set(int, java.lang.Object)
	 */
	@Override
	public SparseBinaryMatrixSupport set(int index, Object value) {
		set(index, ((Integer) value).intValue());
		return this;
	}
    
}
