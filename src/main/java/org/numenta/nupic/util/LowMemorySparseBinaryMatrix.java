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

import java.util.Arrays;

/**
 * Low Memory implementation of {@link SparseBinaryMatrixSupport} without 
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
		int[] slice = new int[dimensions[1]];
		for(int j = 0; j < dimensions[1]; j++) {
			slice[j] = get(coordinates[0], j);
		}
		//Ensure return value is of type Array
		if(!slice.getClass().isArray()) {
			throw new IllegalArgumentException(
					"This method only returns the array holding the specified index: " + 
							Arrays.toString(coordinates));
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
		if (value > 0) {
			super.set(value, coordinates);
			updateTrueCounts(coordinates);
		}
		
		return this;
	}

	@Override
	public LowMemorySparseBinaryMatrix setForTest(int index, int value) {
		if (value > 1) {
			super.setForTest(index, value);
		}
		
		return this;
	}

	private void updateTrueCounts(int... coordinates) {
		int sum = 0;

		for (int j = 0; j < dimensions[1]; j++) {
			sum += getIntValue(coordinates[0], j);
		}

		setTrueCount(coordinates[0],sum);
	}

	@Override
	protected int[] values() {
		int[] dense = new int[getMaxIndex()];
		for (int i = 0; i <= getMaxIndex(); i++) {
			dense[i] = get(i);
		}
		
		return dense;
	}


	@Override
	public LowMemorySparseBinaryMatrix set(int index, Object value) {
		super.set(index, ((Integer) value).intValue());
		return this;
	}


}
