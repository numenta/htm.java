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
 * Support class for flat {@link Matrix} implementations.
 * 
 * @author David Ray
 * @author Jose Luis Martin
 * 
 * @param <T> element type
 */
public abstract class FlatMatrixSupport<T> implements FlatMatrix<T> {

    protected int[] dimensions;
    protected int[] dimensionMultiples;
    protected boolean isColumnMajor;
    protected int numDimensions;

    /**
     * Constructs a new {@link FlatMatrixSupport} object to be configured with specified
     * dimensions and major ordering.
     * @param dimensions  the dimensions of this matrix	
     */
    public FlatMatrixSupport(int[] dimensions) {
        this(dimensions, false);
    }

    /**
     * Constructs a new {@link FlatMatrixSupport} object to be configured with specified
     * dimensions and major ordering.
     * 
     * @param dimensions				the dimensions of this sparse array	
     * @param useColumnMajorOrdering	flag indicating whether to use column ordering or
     * 									row major ordering. if false (the default), then row
     * 									major ordering will be used. If true, then column major
     * 									ordering will be used.
     */
    public FlatMatrixSupport(int[] dimensions, boolean useColumnMajorOrdering) {
        this.dimensions = dimensions;
        this.numDimensions = dimensions.length;
        this.dimensionMultiples = initDimensionMultiples(
                useColumnMajorOrdering ? reverse(dimensions) : dimensions);
        isColumnMajor = useColumnMajorOrdering;
    }

    /**
     * Compute the flat index of a multidimensonal array.
     * @param indexes multidimensional indexes
     * @return the flat array index;
     */
    public int computeIndex(int[] indexes) {
        return computeIndex(indexes, true);
    }

    public int computeIndex(int[] coordinates, boolean doCheck) {
        if(doCheck) checkDims(coordinates);

        int[] localMults = isColumnMajor ? reverse(dimensionMultiples) : dimensionMultiples;
        int base = 0;
        for(int i = 0;i < coordinates.length;i++) {
            base += (localMults[i] * coordinates[i]);
        }
        return base;
    }

    /**
     * Checks the indexes specified to see whether they are within the
     * configured bounds and size parameters of this array configuration.
     * 
     * @param index the array dimensions to check
     */
    protected void checkDims(int[] index) {
        if(index.length != numDimensions) {
            throw new IllegalArgumentException("Specified coordinates exceed the configured array dimensions " +
                    "input dimensions: " + index.length + " > number of configured dimensions: " + numDimensions);
        }
        for(int i = 0;i < index.length - 1;i++) {
            if(index[i] >= dimensions[i]) {
                throw new IllegalArgumentException("Specified coordinates exceed the configured array dimensions " +
                        print1DArray(index) + " > " + print1DArray(dimensions));
            }
        }
    }

    @Override
    public int[] computeCoordinates(int index) {
        int[] returnVal = new int[getNumDimensions()];
        int base = index;
        for(int i = 0;i < dimensionMultiples.length; i++) {
            int quotient = base / dimensionMultiples[i];
            base %= dimensionMultiples[i];
            returnVal[i] = quotient;
        }
        return isColumnMajor ? reverse(returnVal) : returnVal;
    }

    /**
     * Initializes internal helper array which is used for multidimensional
     * index computation.
     * @param dimensions matrix dimensions
     * @return array for use in coordinates to flat index computation.
     */
    protected int[] initDimensionMultiples(int[] dimensions) {
        int holder = 1;
        int len = dimensions.length;
        int[] dimensionMultiples = new int[getNumDimensions()];
        for(int i = 0;i < len;i++) {
            holder *= (i == 0 ? 1 : dimensions[len - i]);
            dimensionMultiples[len - 1 - i] = holder;
        }
        return dimensionMultiples;
    }

    /**
     * Utility method to shrink a single dimension array by one index.
     * @param array the array to shrink
     * @return
     */
    protected int[] copyInnerArray(int[] array) {
        if(array.length == 1) return array;

        int[] retVal = new int[array.length - 1];
        System.arraycopy(array, 1, retVal, 0, array.length - 1);
        return retVal;
    }

    /**
     * Reverses the specified array.
     * @param input
     * @return
     */
    public static int[] reverse(int[] input) {
        int[] retVal = new int[input.length];
        for(int i = input.length - 1, j = 0;i >= 0;i--, j++) {
            retVal[j] = input[i];
        }
        return retVal;
    }

    /**
     * Prints the specified array to a returned String.
     * 
     * @param aObject   the array object to print.
     * @return  the array in string form suitable for display.
     */
    public static String print1DArray(Object aObject) {
        if (aObject.getClass().isArray()) {
            if (aObject instanceof Object[]) // can we cast to Object[]
                return Arrays.toString((Object[]) aObject);
            else {  // we can't cast to Object[] - case of primitive arrays
                int length = Array.getLength(aObject);
                Object[] objArr = new Object[length];
                for (int i=0; i<length; i++)
                    objArr[i] =  Array.get(aObject, i);
                return Arrays.toString(objArr);
            }
        }
        return "[]";
    }

    @Override
    public abstract T get(int index);

    @Override 
    public abstract FlatMatrixSupport<T> set(int index, T value);

    @Override
    public T get(int... indexes) {
        return get(computeIndex(indexes));
    }

    @Override
    public FlatMatrixSupport<T> set(int[] indexes, T value) {
        set(computeIndex(indexes), value); 
        return this;
    }

    public int getSize() {
        return Arrays.stream(this.dimensions).reduce((n,i) -> n*i).getAsInt();
    }

    @Override
    public int getMaxIndex() {
        return getDimensions()[0] * Math.max(1, getDimensionMultiples()[0]) - 1;
    }

    @Override
    public int[] getDimensions() {
        return this.dimensions;
    }

    public void setDimensions(int[] dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public int getNumDimensions() {
        return this.dimensions.length;
    }

    @Override
    public int[] getDimensionMultiples() {
        return this.dimensionMultiples;
    }

}
