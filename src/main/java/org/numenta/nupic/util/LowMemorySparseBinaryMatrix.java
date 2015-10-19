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

package org.numenta.nupic.util;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Low Memory implementation of {@link SparseBinaryMatrix} without 
 * a backing array.
 * 
 * @author Jose Luis Martin
 */
public class LowMemorySparseBinaryMatrix extends SparseBinaryMatrixSupport {

	public LowMemorySparseBinaryMatrix(int[] dimensions) {
		this(dimensions, false);
	}

	public LowMemorySparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
		super(dimensions, useColumnMajorOrdering);
	}
	
	@Override
	public Object getSlice(int... coordinates) {
		int[] dimensions = getDimensions();
		// check for valid coordinates
		if (coordinates.length >= dimensions.length)
			sliceError(coordinates);
		
		int sliceDimensionsLength = dimensions.length - coordinates.length;
		int[] sliceDimensions = (int[]) Array.newInstance(int.class, sliceDimensionsLength);
		
		for (int i = coordinates.length ; i < dimensions.length; i++) 
			sliceDimensions[i - coordinates.length] = dimensions[i];
		
		int[] elementCoordinates = Arrays.copyOf(coordinates, coordinates.length + 1);
		Object slice = Array.newInstance(int.class, sliceDimensions);
		
		if (coordinates.length + 1 == dimensions.length) {
			// last slice 
			for (int i = 0; i < dimensions[coordinates.length]; i++) {
				elementCoordinates[coordinates.length] = i;
				Array.set(slice,  i, get(elementCoordinates));
			}
		}
		else {
			for (int i = 0; i < dimensions[sliceDimensionsLength]; i++) {
				elementCoordinates[coordinates.length] = i;
				Array.set(slice, i, getSlice(elementCoordinates));
			}
		}
		
		return slice;
	}

	@Override
	public void rightVecSumAtNZ(int[] inputVector, int[] results) {
		if (this.dimensions.length > 1) {
			for(int i = 0; i < this.dimensions[0]; i++) {
				for(int j = 0;  j < this.dimensions[1] ; j++) {
					results[i] += (inputVector[j] * (int) get(i, j));
				}
			}
		}
		else {
			for(int i = 0; i < this.dimensions[0]; i++) {
				results[0] += (inputVector[i] * (int) get(i));
			}
			
			for (int i = 0; i < this.dimensions[0]; i++) {
				results[i] = results[0];
			}
		}
	}

	@Override
	public LowMemorySparseBinaryMatrix set(int value, int... coordinates) {
		super.set(value, coordinates);
		updateTrueCounts(coordinates);
		
		return this;
	}

	@Override
	public LowMemorySparseBinaryMatrix setForTest(int index, int value) {
		if (value > 1) {
			super.setForTest(index, value);
		}
		
		return this;
	}

	/**
	 * Update the true counts for a coordinates.
	 * @param coordinates
	 */
	private void updateTrueCounts(int... coordinates) {
		Object slice = getSlice(coordinates[0]);
		int sum = ArrayUtils.aggregateArray(slice);
		setTrueCount(coordinates[0],sum);
	}

	@Override
	public LowMemorySparseBinaryMatrix set(int index, Object value) {
		super.set(index, ((Integer) value).intValue());
		return this;
	}


}
