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

import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.Persistable;

import gnu.trove.set.hash.TIntHashSet;

/**
 * Fast implementation of {@link SparseBinaryMatrix} for use as ConnectedMatrix in  
 * {@link Connections}
 * 
 * @author Jose Luis Martin
 */
public class FastConnectionsMatrix extends AbstractSparseBinaryMatrix implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private TIntHashSet[] columns;
   
    /**
     * @param dimensions
     */
    public FastConnectionsMatrix(int[] dimensions) {
        this(dimensions, false);
    }
    
    /**
     * @param dimensions
     * @param useColumnMajorOrdering
     */
    public FastConnectionsMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        this.columns = new TIntHashSet[dimensions[0]];
    }

    @Override
    public Object getSlice(int... coordinates) {
        if (coordinates.length > this.numDimensions - 1)
            sliceError(coordinates);
        
        int[] slice = new int[this.dimensions[1]];
        for (int i = 0; i < this.dimensions[1]; i++)
            slice[i] = this.columns[coordinates[0]].contains(i) ? 1 : 0;
        
        return slice;
    }

    @Override
    public void rightVecSumAtNZ(int[] inputVector, int[] results) {
        for (int i = 0; i < dimensions[0]; i++) {
            for (int index : getColumnInput(i).toArray()) {
                if (inputVector[index] != 0)
                    results[i] += 1;
            }
        }
    }
    
    @Override
    public void rightVecSumAtNZ(int[] inputVector, int[] results, double stimulusThreshold) {
        for (int i = 0; i < dimensions[0]; i++) {
            int[] columnIndexes = getColumnInput(i).toArray();
            for (int j = 0;j < columnIndexes.length;j++) {
                if (inputVector[columnIndexes[j]] != 0) {
                    results[i] += 1;
                }
                if(j == columnIndexes.length - 1 && results[i] < stimulusThreshold) {
                    results[i] = 0;
                }
            }
        }
    }

    @Override
    public FastConnectionsMatrix set(int index, Object value) {
       set(index, ((Integer)value).intValue());
       return this;
    }
    
    @Override
    public AbstractSparseBinaryMatrix set(int value, int... coordinates) {
        TIntHashSet input = getColumnInput(coordinates[0]);
        if (value == 0) {
            input.remove(coordinates[1]);
        }
        else {
            input.add(coordinates[1]);
        }
        
        return this;
    }
    
    

    @Override
    public Integer get(int index) {
        int[] coordinates = computeCoordinates(index);
        return  getColumnInput(coordinates[0]).contains(coordinates[1]) ? 1 : 0;
    }

    /**
     * @param i
     */
    private TIntHashSet getColumnInput(int i) {
        if (this.columns[i] == null)
            this.columns[i] = new TIntHashSet();
        
        return this.columns[i];
        
    }

    @Override
    public void clearStatistics(int row) {
        getColumnInput(row).clear();
    }

    @Override
    public int getTrueCount(int index) {
        return getColumnInput(index).size();
    }

    @Override
    public int[] getTrueCounts() {
        int[] trueCounts = new int[this.dimensions[0]];
        for (int i = 0; i < this.dimensions[0]; i++) 
            trueCounts[i] = getTrueCount(i);
        
        return trueCounts;
    }

    @Override
    public AbstractSparseBinaryMatrix setForTest(int index, int value) {
        return set(index, value);
    }
}
