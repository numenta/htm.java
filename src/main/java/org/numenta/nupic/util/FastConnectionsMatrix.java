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

import org.numenta.nupic.Connections;

import gnu.trove.map.hash.TIntByteHashMap;

/**
 * Fast implementation of {@link SparseBinaryMatrix} for use as ConnectedMatrix in  
 * {@link Connections}
 * 
 * @author Jose Luis Martin
 */
public class FastConnectionsMatrix extends SparseBinaryMatrixSupport {
    
    private TIntByteHashMap[] columns;
   
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
        this.columns = new TIntByteHashMap[dimensions[0]];
    }

    @Override
    public Object getSlice(int... coordinates) {
        if (coordinates.length > this.numDimensions - 1)
            sliceError(coordinates);
        
        int[] slice = new int[this.dimensions[1]];
        for (int i = 0; i < this.dimensions[1]; i++)
            slice[i] = this.columns[coordinates[0]].get(i);
        
        return slice;
    }

    @Override
    public void rightVecSumAtNZ(int[] inputVector, int[] results) {
        for (int i = 0; i < dimensions[0]; i++) {
            for (int index : getMap(i).keys()) {
                if (inputVector[index] != 0)
                    results[i] += 1;
            }
        }
    }

    @Override
    public FastConnectionsMatrix set(int index, Object value) {
       set(index, ((Integer)value).intValue());
       return this;
    }
    
    @Override
    public SparseBinaryMatrixSupport set(int value, int... coordinates) {
        TIntByteHashMap map = getMap(coordinates[0]);
        if (value == 0) {
            map.remove(coordinates[1]);
        }
        else {
            map.put(coordinates[1], (byte) value);
        }
        
        return this;
    }
    
    

    @Override
    public Integer get(int index) {
        int[] coordinates = computeCoordinates(index);
        return (int) getMap(coordinates[0]).get(coordinates[1]);
    }

    /**
     * @param i
     */
    private TIntByteHashMap getMap(int i) {
        if (this.columns[i] == null)
            this.columns[i] = new TIntByteHashMap();
        
        return this.columns[i];
        
    }

    @Override
    public void clearStatistics(int row) {
        getMap(row).clear();
    }

    @Override
    public int getTrueCount(int index) {
        return getMap(index).size();
    }

    @Override
    public int[] getTrueCounts() {
        int[] trueCounts = new int[this.dimensions[0]];
        for (int i = 0; i < this.dimensions[0]; i++) 
            trueCounts[i] = getTrueCount(i);
        
        return trueCounts;
    }
}
