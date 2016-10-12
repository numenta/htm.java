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

import org.numenta.nupic.model.Persistable;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Base class for matrices containing specifically binary (0 or 1) integer values
 * 
 * @author David Ray
 * @author Jose Luis Martin
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractSparseBinaryMatrix extends AbstractSparseMatrix implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private int[] trueCounts;

    /**
     * Constructs a new {@code AbstractSparseBinaryMatrix} with the specified
     * dimensions (defaults to row major ordering)
     * 
     * @param dimensions    each indexed value is a dimension size
     */
    public AbstractSparseBinaryMatrix(int[] dimensions) {
        this(dimensions, false);
    }

    /**
     * Constructs a new {@code AbstractSparseBinaryMatrix} with the specified dimensions,
     * allowing the specification of column major ordering if desired. 
     * (defaults to row major ordering)
     * 
     * @param dimensions                each indexed value is a dimension size
     * @param useColumnMajorOrdering    if true, indicates column first iteration, otherwise
     *                                  row first iteration is the default (if false).
     */
    public AbstractSparseBinaryMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        this.trueCounts = new int[dimensions[0]];
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
    public abstract Object getSlice(int... coordinates);

    /**
     * Launch getSlice error, to share it with subclass {@link #getSlice(int...)}
     * implementations.
     * @param coordinates
     */
    protected void sliceError(int... coordinates) {
        throw new IllegalArgumentException(
            "This method only returns the array holding the specified maximum index: " + 
                    Arrays.toString(dimensions));
    }
    
    /**
     * Calculate the flat indexes of a slice
     * @return the flat indexes array
     */
    protected int[] getSliceIndexes(int[] coordinates) {
        int[] dimensions = getDimensions();
        // check for valid coordinates
        if (coordinates.length >= dimensions.length) {
            sliceError(coordinates);
        }

        int sliceDimensionsLength = dimensions.length - coordinates.length;
        int[] sliceDimensions = (int[]) Array.newInstance(int.class, sliceDimensionsLength);

        for (int i = coordinates.length ; i < dimensions.length; i++) { 
            sliceDimensions[i - coordinates.length] = dimensions[i];
        }

        int[] elementCoordinates = Arrays.copyOf(coordinates, coordinates.length + 1);
        int sliceSize = Arrays.stream(sliceDimensions).reduce((n,i) -> n*i).getAsInt();
        int[] slice = new int[sliceSize];

        if (coordinates.length + 1 == dimensions.length) {
            // last slice 
            for (int i = 0; i < dimensions[coordinates.length]; i++) {
                elementCoordinates[coordinates.length] = i;
                Array.set(slice,  i, computeIndex(elementCoordinates));
            }
        }
        else {
            for (int i = 0; i < dimensions[sliceDimensionsLength]; i++) {
                elementCoordinates[coordinates.length] = i;
                int[] indexes = getSliceIndexes(elementCoordinates);
                System.arraycopy(indexes, 0, slice, i*indexes.length, indexes.length);
            }
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
    public abstract void rightVecSumAtNZ(int[] inputVector, int[] results);
    
    /**
     * Fills the specified results array with the result of the 
     * matrix vector multiplication.
     * 
     * @param inputVector       the right side vector
     * @param results           the results array
     */
    public abstract void rightVecSumAtNZ(int[] inputVector, int[] results, double stimulusThreshold);
        
    /**
     * Sets the value at the specified index.
     * 
     * @param index     the index the object will occupy
     * @param object    the object to be indexed.
     */
    @Override
    public AbstractSparseBinaryMatrix set(int index, int value) {
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
    public abstract AbstractSparseBinaryMatrix set(int value, int... coordinates);

    /**
     * Sets the specified values at the specified indexes.
     * 
     * @param indexes   indexes of the values to be set
     * @param values    the values to be indexed.
     * 
     * @return this {@code SparseMatrix} implementation
     */
    public AbstractSparseBinaryMatrix set(int[] indexes, int[] values) { 
        for(int i = 0;i < indexes.length;i++) {
            set(indexes[i], values[i]);
        }
        return this;
    }


    public Integer get(int... coordinates) {
        return get(computeIndex(coordinates));
    }

    public abstract Integer get(int index);
    
    /**
     * Sets the value at the specified index skipping the automatic
     * truth statistic tallying of the real method.
     * 
     * @param index     the index the object will occupy
     * @param object    the object to be indexed.
     */
    public abstract AbstractSparseBinaryMatrix setForTest(int index, int value);

    /**
     * Call This for TEST METHODS ONLY
     * Sets the specified values at the specified indexes.
     * 
     * @param indexes   indexes of the values to be set
     * @param values    the values to be indexed.
     * 
     * @return this {@code SparseMatrix} implementation
     */
    public AbstractSparseBinaryMatrix set(int[] indexes, int[] values, boolean isTest) { 
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
        trueCounts[row] = 0;
        
        for (int index : getSliceIndexes(new int[] { row })) {
            set(index, 0);
        }
    }

    /**
     * Returns the int value at the index computed from the specified coordinates
     * @param coordinates   the coordinates from which to retrieve the indexed object
     * @return  the indexed object
     */
    public int getIntValue(int... coordinates) {
        return get(computeIndex(coordinates));
    }

    /**
     * Returns the T at the specified index.
     * 
     * @param index     the index of the T to return
     * @return  the T at the specified index.
     */
    @Override
    public int getIntValue(int index) {
        return get(index);
    }

    /**
     * Returns a sorted array of occupied indexes.
     * @return  a sorted array of occupied indexes.
     */
    @Override
    public int[] getSparseIndices() {
        TIntList indexes = new TIntArrayList();
        for (int i = 0; i <= getMaxIndex(); i ++) {
            if (get(i) > 0) {
                indexes.add(i);
            }
        }
        
        return indexes.toArray();
    }

    /**
     * This {@code SparseBinaryMatrix} will contain the operation of or-ing
     * the inputMatrix with the contents of this matrix; returning this matrix
     * as the result.
     * 
     * @param inputMatrix   the matrix containing the "on" bits to or
     * @return  this matrix
     */
    public AbstractSparseBinaryMatrix or(AbstractSparseBinaryMatrix inputMatrix) {
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
    public AbstractSparseBinaryMatrix or(TIntCollection onBitIndexes) {
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
    public AbstractSparseBinaryMatrix or(int[] onBitIndexes) {
        int[] ones = new int[onBitIndexes.length];
        Arrays.fill(ones, 1);
        return set(onBitIndexes, ones);
    }
    
    protected TIntSet getSparseSet() {
        return new TIntHashSet(getSparseIndices());
    }

    /**
     * Returns true if the on bits of the specified matrix are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean all(AbstractSparseBinaryMatrix matrix) {
        return getSparseSet().containsAll(matrix.getSparseIndices());
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
        return getSparseSet().containsAll(onBits);
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
        return getSparseSet().containsAll(onBits);
    }

    /**
     * Returns true if any of the on bits of the specified matrix are
     * matched by the on bits of this matrix. It is allowed that 
     * this matrix have more on bits than the specified matrix.
     * 
     * @param matrix
     * @return
     */
    public boolean any(AbstractSparseBinaryMatrix matrix) {
        TIntSet keySet = getSparseSet();
        
        for(int i : matrix.getSparseIndices()) {
            if(keySet.contains(i)) return true;
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
        TIntSet keySet = getSparseSet();
        
        for(TIntIterator i = onBits.iterator();i.hasNext();) {
            if(keySet.contains(i.next())) return true;
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
        TIntSet keySet = getSparseSet();
        
        for(int i : onBits) {
            if(keySet.contains(i)) return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(trueCounts);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(!super.equals(obj))
            return false;
        if(getClass() != obj.getClass())
            return false;
        AbstractSparseBinaryMatrix other = (AbstractSparseBinaryMatrix)obj;
        if(!Arrays.equals(trueCounts, other.trueCounts))
            return false;
        return true;
    }
}
