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

/**
 * Base interface for Matrices.
 * 
 * @author Jose Luis Martin.
 * 
 * @param <T> element type
 */
public interface Matrix<T> {

    /**
     * Returns the array describing the dimensionality of the configured array.
     * @return  the array describing the dimensionality of the configured array.
     */
    int[] getDimensions();

    /**
     * Returns the configured number of dimensions.
     * @return  the configured number of dimensions.
     */
    int getNumDimensions();

    /**
     * Gets element at supplied index.
     * @param index index to retrieve.
     * @return element at index.
     */
    T get(int... index);

    /**
     * Puts an element to supplied index.
     * @param index index to put on.
     * @param value value element.
     */
    Matrix<T> set(int[] index, T value);
}
