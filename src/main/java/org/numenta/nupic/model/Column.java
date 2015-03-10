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

package org.numenta.nupic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.numenta.nupic.Connections;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Abstraction of both an input bit and a columnal collection of
 * {@link Cell}s which have behavior associated with membership to
 * a given {@code Column}
 * 
 * @author Chetan Surpur
 * @author David Ray
 *
 */
public class Column {
    /** The flat non-topological index of this column */
    private final int index;
    /** Configuration of cell count */
    private final int numCells;
    /** Connects {@link SpatialPooler} input pools */
    private ProximalDendrite proximalDendrite;

    private Cell[] cells;

    /**
     * Constructs a new {@code Column}
     * 
     * @param numCells      number of cells per column
     * @param index         the index of this column
     */
    public Column(int numCells, int index) {
        this.numCells = numCells;
        this.index = index;
        cells = new Cell[numCells];
        for(int i = 0;i < numCells;i++) {
            cells[i] = new Cell(this, i);
        }
        proximalDendrite = new ProximalDendrite(index);
    }

    /**
     * Returns the {@link Cell} residing at the specified index.
     * 
     * @param index     the index of the {@link Cell} to return.
     * @return          the {@link Cell} residing at the specified index.
     */
    public Cell getCell(int index) {
        return cells[index];
    }

    /**
     * Returns a {@link List} view of this {@code Column}'s {@link Cell}s.
     * @return
     */
    public List<Cell> getCells() {
        return Arrays.asList(cells);
    }

    /**
     * Returns the index of this {@code Column}
     * @return  the index of this {@code Column}
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the configured number of cells per column for
     * all {@code Column} objects within the current {@link TemporalMemory}
     * @return
     */
    public int getNumCellsPerColumn() {
        return numCells;
    }

    /**
     * Returns the {@link Cell} with the least number of {@link DistalDendrite}s.
     * 
     * @param c         the connections state of the temporal memory
     * @param random
     * @return
     */
    public Cell getLeastUsedCell(Connections c, Random random) {
        List<Cell> cells = getCells();
        List<Cell> leastUsedCells = new ArrayList<Cell>();
        int minNumSegments = Integer.MAX_VALUE;

        for(Cell cell : cells) {
            int numSegments = cell.getSegments(c).size();

            if(numSegments < minNumSegments) {
                minNumSegments = numSegments;
                leastUsedCells.clear();
            }

            if(numSegments == minNumSegments) {
                leastUsedCells.add(cell);
            }
        }
        int index = random.nextInt(leastUsedCells.size());
        Collections.sort(leastUsedCells);
        return leastUsedCells.get(index); 
    }

    /**
     * Returns this {@code Column}'s single {@link ProximalDendrite}
     * @return
     */
    public ProximalDendrite getProximalDendrite() {
        return proximalDendrite;
    }

    /**
     * Delegates the potential synapse creation to the one {@link ProximalDendrite}.
     * 
     * @param c						the {@link Connections} memory
     * @param inputVectorIndexes	indexes specifying the input vector bit
     */
    public Pool createPotentialPool(Connections c, int[] inputVectorIndexes) {
        return proximalDendrite.createPool(c, inputVectorIndexes);
    }

    /**
     * Sets the permanences on the {@link ProximalDendrite} {@link Synapse}s
     * 
     * @param c				the {@link Connections} memory object
     * @param permanences	floating point degree of connectedness
     */
    public void setProximalPermanences(Connections c, double[] permanences) {
        proximalDendrite.setPermanences(c, permanences);
    }

    /**
     * Sets the permanences on the {@link ProximalDendrite} {@link Synapse}s
     * 
     * @param c				the {@link Connections} memory object
     * @param permanences	floating point degree of connectedness
     */
    public void setProximalPermanencesSparse(Connections c, double[] permanences, int[] indexes) {
        proximalDendrite.setPermanences(c, permanences, indexes);
    }

    /**
     * Delegates the call to set synapse connected indexes to this 
     * {@code Column}'s {@link ProximalDendrite}
     * @param c
     * @param connections
     */
    public void setProximalConnectedSynapsesForTest(Connections c, int[] connections) {
        proximalDendrite.setConnectedSynapsesForTest(c, connections);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Column: idx=" + index;
    }
}
