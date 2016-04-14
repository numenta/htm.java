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
package org.numenta.nupic;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;

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
     * Converts a {@link Collection} of {@link Cell}s to a list
     * of cell indexes.
     * 
     * @param cells
     * @return
     */
    public static int[] asCellIndices(Collection<Cell> cells) {
        try { 
            // This ugliness is inserted because, while there is no sharing by different threads and
            // and no reentrant access, JUnit tests involving many tests make this throw a 
            // ConcurrentModificationException even though this code is isolated???? 
            // In that case, running it twice corrects the internal modCount. :-(
            return cells.stream().mapToInt(cell -> cell.getIndex()).sorted().toArray();
        }catch(Exception e) {
            return cells.stream().mapToInt(cell -> cell.getIndex()).sorted().toArray();
        }
    }
}