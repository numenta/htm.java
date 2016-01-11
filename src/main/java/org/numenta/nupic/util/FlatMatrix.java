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
 * Provides contract for flat matrix implementations.
 * 
 * @author David Ray
 * @author Jose Luis Martin
 */
public interface FlatMatrix<T> extends Matrix<T> {

    T get(int index);

    FlatMatrix<T> set(int index, T value);

    int computeIndex(int[] coordinates);

    /**
     * Returns the maximum accessible flat index.
     * @return  the maximum accessible flat index.
     */
    int getMaxIndex();

    public int computeIndex(int[] coordinates, boolean doCheck);

    /**
     * Returns an integer array representing the coordinates of the specified index
     * in terms of the configuration of this {@code SparseMatrix}.
     * @param index the flat index to be returned as coordinates
     * @return  coordinates
     */
    int[] computeCoordinates(int index);

    int[] getDimensionMultiples();
}
