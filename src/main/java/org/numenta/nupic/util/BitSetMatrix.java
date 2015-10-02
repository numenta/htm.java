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
import java.util.BitSet;

/**
 * {@link FlatMatrix} implementation that store booleans in a {@link BitSet}.
 * 
 * @author Jose Luis Martin
 */
public class BitSetMatrix extends FlatMatrixSupport<Boolean> {

	private BitSet data;
	
	/**
	 * @param dimensions
	 */
	public BitSetMatrix(int[] dimensions) {
		super(dimensions);
	}

	public BitSetMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
		super(dimensions, useColumnMajorOrdering);
		this.data = new BitSet(Arrays.stream(dimensions).reduce((n,i) -> n*i).getAsInt());
	}
	
	@Override
	public Boolean get(int... coordinates) {
		return get(computeIndex(coordinates));
	}
	
	@Override
	public Boolean get(int index) {
		return this.data.get(index);
	}

	@Override
	public Matrix<Boolean> set(int[] coordinates, Boolean value) {
		this.data.set(computeIndex(coordinates), value);
		return this;
	}

	@Override
	public FlatMatrix<Boolean> set(int index, Boolean value) {
		this.data.set(index, value);
		return this;
	}
}
