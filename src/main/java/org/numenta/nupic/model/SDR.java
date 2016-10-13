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
package org.numenta.nupic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * <p>
 * For now, a utility class for convenience operations
 * on integer arrays understood to be algorithmic inputs
 * and outputs; and conversions to and from canonical objects.
 * </p><p>
 * Later, this may become the encapsulation of the vectors
 * representing SDRs and previously treated as integer arrays.
 * </p><p>
 * <b>NOTE:</b> <em>Eclipse is not up to date with its leakable resource inspection.
 * Streams not derived from channels (i.e. from arrays or lists) do not 
 * need explicit closing.</em>
 * </p>
 * <p>
 * see here: http://stackoverflow.com/questions/25796118/java-8-streams-and-try-with-resources 
 * </p>
 * @author cogmission
 */
public class SDR {
    
    /**
     * Converts a vector of {@link Cell} indexes to {@link Column} indexes.
     * 
     * @param cells             the indexes of the cells to convert
     * @param cellsPerColumn    the defined number of cells per column  
     *                          false if not.   
     * @return  the column indexes of the specified cells.
     */
    public static int[] asColumnIndices(int[] cells, int cellsPerColumn) {
        IntStream op = Arrays.stream(cells);
        return op.map(cell -> cell / cellsPerColumn).distinct().toArray();
    }
    
    /**
     * Converts a vector of {@link Cell} indexes to {@link Column} indexes.
     * 
     * @param cells             the indexes of the cells to convert
     * @param cellsPerColumn    the defined number of cells per column  
     *                          false if not.   
     * @return  the column indexes of the specified cells.
     */
    public static int[] asColumnIndices(List<Integer> cells, int cellsPerColumn) {
        IntStream op = cells.stream().mapToInt(c -> c);
        return op.map(cellIdx -> cellIdx / cellsPerColumn).distinct().toArray();
    }
    
    /**
     * Converts a List of {@link Cell}s to {@link Column} indexes.
     * 
     * @param cells             the list of cells to convert
     * @param cellsPerColumn    the defined number of cells per column  
     *                          false if not.   
     * @return  the column indexes of the specified cells.
     */
    public static int[] cellsToColumns(List<Cell> cells, int cellsPerColumn) {
        IntStream op = cells.stream().mapToInt(c -> c.getIndex());
            
        return op.map(cellIdx -> cellIdx / cellsPerColumn).distinct().toArray();
    }
    
    /**
     * Converts a Set of {@link Cell}s to {@link Column} indexes.
     * 
     * @param cells             the list of cells to convert
     * @param cellsPerColumn    the defined number of cells per column  
     * 
     * @return  the column indexes of the specified cells.
     */
    public static int[] cellsAsColumnIndices(Set<Cell> cells, int cellsPerColumn) {
        return cells.stream().mapToInt(c -> c.getIndex())
                   .sorted().map(cellIdx -> cellIdx / cellsPerColumn).distinct().toArray();
    }
    
    /**
     * Converts a Collection of {@link Cell}s to {@link Column} indexes.
     *
     * @param cells             the list of cells to convert
     *
     * @return  sorted array of column indices.
     */
    public static int[] asColumnList(Collection<Cell> cells) {
        return cells.stream().mapToInt(c -> c.getColumn().getIndex()).sorted().distinct().toArray();
    }

    /**
     * Converts a {@link Collection} of {@link Cell}s to a list
     * of cell indexes.
     * 
     * @param cells
     * @return
     */
    public static int[] asCellIndices(Collection<Cell> cells) {
        int[] retVal = new int[cells.size()];
        int i = 0;
        // Prevent ridiculous ConcurrentModificationException since "reads" mark as modifications????
        List<Cell> newCells = new ArrayList<>(cells);
        for(Cell cell : newCells) {
            retVal[i++] = cell.getIndex();
        }
        return retVal;
    }
}