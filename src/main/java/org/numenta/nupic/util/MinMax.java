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

import org.numenta.nupic.model.Persistable;

/**
 * Holds two values, a min and a max. Can later be developed to
 * employ operations on those values (i.e. distance etc.)
 * 
 * @author David Ray
 */
public class MinMax implements Persistable {
	private static final long serialVersionUID = 1L;
    
	private double min;
	private double max;
	
	/**
	 * Constructs a new {@code MinMax} instance
	 */
	public MinMax(){}
	/**
	 * Constructs a new {@code MinMax} instance
	 * 
	 * @param min	the minimum or lower bound
	 * @param max	the maximum or upper bound
	 */
	public MinMax(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	/**
	 * Returns the configured min value
	 */
	public double min() {
		return min;
	}
	
	/**
	 * Returns the configured max value
	 */
	public double max() {
		return max;
	}
	
	@Override
	public String toString() {
		return new StringBuilder().append(min).
			append(", ").append(max).toString();
	}
}
