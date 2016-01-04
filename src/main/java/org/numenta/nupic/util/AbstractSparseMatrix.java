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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.io.Serializable;
import java.lang.reflect.Array;

/**
 * Allows storage of array data in sparse form, meaning that the indexes
 * of the data stored are maintained while empty indexes are not. This allows
 * savings in memory and computational efficiency because iterative algorithms
 * need only query indexes containing valid data. The dimensions of matrix defined
 * at construction time and immutable - matrix fixed size data structure.
 * 
 * @author David Ray
 * @author Jose Luis Martin
 *
 * @param <T>
 */
public abstract class AbstractSparseMatrix<T> extends AbstractFlatMatrix<T> implements SparseMatrix<T>, Serializable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new {@code AbstractSparseMatrix} with the specified
     * dimensions (defaults to row major ordering)
     * 
     * @param dimensions    each indexed value is a dimension size
     */
    public AbstractSparseMatrix(int[] dimensions) {
        this(dimensions, false);
    }

    /**
     * Constructs a new {@code AbstractSparseMatrix} with the specified dimensions,
     * allowing the specification of column major ordering if desired. 
     * (defaults to row major ordering)
     * 
     * @param dimensions                each indexed value is a dimension size
     * @param useColumnMajorOrdering    if true, indicates column first iteration, otherwise
     *                                  row first iteration is the default (if false).
     */
    public AbstractSparseMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
    }

    /**
     * Sets the object to occupy the specified index.
     * 
     * @param index     the index the object will occupy
     * @param value     the value to be indexed.
     * 
     * @return this {@code SparseMatrix} implementation
     */
    protected <S extends AbstractSparseMatrix<T>> S set(int index, int value) { return null; }

    /**
     * Sets the object to occupy the specified index.
     * 
     * @param index     the index the object will occupy
     * @param value     the value to be indexed.
     * 
     * @return this {@code SparseMatrix} implementation
     */
    protected <S extends AbstractSparseMatrix<T>> S set(int index, double value) { return null; }

    /**
     * Sets the specified object to be indexed at the index
     * computed from the specified coordinates.
     * @param object        the object to be indexed.
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     * 
     * @return this {@code SparseMatrix} implementation
     */
    @Override
    public AbstractSparseMatrix<T> set(int[] coordinates, T object) { return null; }

    /**
     * Sets the specified object to be indexed at the index
     * computed from the specified coordinates.
     * @param value         the value to be indexed.
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     * 
     * @return this {@code SparseMatrix} implementation
     */
    protected <S extends AbstractSparseMatrix<T>> S set(int value, int... coordinates) { return null; }

    /**
     * Sets the specified object to be indexed at the index
     * computed from the specified coordinates.
     * @param value         the value to be indexed.
     * @param coordinates   the row major coordinates [outer --> ,...,..., inner]
     * 
     * @return this {@code SparseMatrix} implementation
     */
    protected <S extends AbstractSparseMatrix<T>> S set(double value, int... coordinates) { return null; }

    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    protected T getObject(int index) { return null; }

    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    protected int getIntValue(int index) { return -1; }

    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    protected double getDoubleValue(int index) { return -1.0; }

    /**
     * Returns the T at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    public T get(int... coordinates) { return null; }

    /**
     * Returns the int value at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    protected int getIntValue(int... coordinates) { return -1; }

    /**
     * Returns the double value at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    protected double getDoubleValue(int... coordinates) { return -1.0; }

    @Override
    public int[] getSparseIndices() { 
        return null;
    }

    @Override
    public int[] get1DIndexes() {
        TIntList results = new TIntArrayList(getMaxIndex() + 1);
        visit(getDimensions(), 0, new int[getNumDimensions()], results);
        return results.toArray();
    }

    /**
     * Recursively loops through the matrix dimensions to fill the results
     * array with flattened computed array indexes.
     * 
     * @param bounds
     * @param currentDimension
     * @param p
     * @param results
     */
    private void visit(int[] bounds, int currentDimension, int[] p, TIntList results) {
        for (int i = 0; i < bounds[currentDimension]; i++) {
            p[currentDimension] = i;
            if (currentDimension == p.length - 1) {
                results.add(computeIndex(p));
            }
            else visit(bounds, currentDimension + 1, p, results);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] asDense(TypeFactory<T> factory) {
        int[] dimensions = getDimensions();
        T[] retVal = (T[])Array.newInstance(factory.typeClass(), dimensions);
        fill(factory, 0, dimensions, dimensions[0], retVal);

        return retVal;
    }

    /**
     * Uses reflection to create and fill a dynamically created multidimensional array.
     * 
     * @param f                 the {@link TypeFactory}
     * @param dimensionIndex    the current index into <em>this class's</em> configured dimensions array
     *                          <em>*NOT*</em> the dimensions used as this method's argument    
     * @param dimensions        the array specifying remaining dimensions to create
     * @param count             the current dimensional size
     * @param arr               the array to fill
     * @return a dynamically created multidimensional array
     */
    @SuppressWarnings("unchecked")
    protected Object[] fill(TypeFactory<T> f, int dimensionIndex, int[] dimensions, int count, Object[] arr) {
        if(dimensions.length == 1) {
            for(int i = 0;i < count;i++) {
                arr[i] = f.make(getDimensions());
            }
            return arr;
        }else{
            for(int i = 0;i < count;i++) {
                int[] inner = copyInnerArray(dimensions);
                T[] r = (T[])Array.newInstance(f.typeClass(), inner);
                arr[i] = fill(f, dimensionIndex + 1, inner, getDimensions()[dimensionIndex + 1], r);
            }
            return arr;
        }
    }

}
