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

import java.util.List;
import java.util.Set;

import org.numenta.nupic.Connections;

/**
 * Software implementation of a neuron in the neocortical region.
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class Cell implements Comparable<Cell> {
    /** This cell's index */
    private final int index;
    /** Remove boxing where necessary */
    final Integer boxedIndex;
    /** The owning {@link Column} */
    private final Column parentColumn;


    /**
     * Constructs a new {@code Cell} object
     * @param column    the parent {@link Column}
     * @param colSeq    this index of this {@code Cell} within its column
     */
    public Cell(Column column, int colSeq) {
        this.parentColumn = column;
        this.index = column.getIndex() * column.getNumCellsPerColumn() + colSeq;
        this.boxedIndex = new Integer(colSeq);
    }

    /**
     * Returns this {@code Cell}'s index.
     * @return
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the column within which this cell resides
     * @return
     */
    public Column getParentColumn() {
        return parentColumn;
    }

    /**
     * Adds a {@link Synapse} which is the receiver of signals
     * from this {@code Cell}
     * 
     * @param c     the connections state of the temporal memory
     * @param s
     */
    public void addReceptorSynapse(Connections c, Synapse s) {
        c.getReceptorSynapses(this).add(s);
    }

    /**
     * Returns the Set of {@link Synapse}s which have this cell
     * as their source cells.
     *  
     * @param   c       the connections state of the temporal memory
     * @return  the Set of {@link Synapse}s which have this cell
     *          as their source cells.
     */
    public Set<Synapse> getReceptorSynapses(Connections c) {
        return c.getReceptorSynapses(this);
    }

    /**
     * Returns a newly created {@link DistalDendrite}
     * 
     * @param   c       the connections state of the temporal memory
     * @param index     the index of the new {@link DistalDendrite}
     * @return          a newly created {@link DistalDendrite}
     */
    public DistalDendrite createSegment(Connections c, int index) {
        DistalDendrite dd = new DistalDendrite(this, index);
        c.getSegments(this).add(dd);

        return dd;
    }

    /**
     * Returns a {@link List} of this {@code Cell}'s {@link DistalDendrite}s
     * 
     * @param   c   the connections state of the temporal memory
     * @return  a {@link List} of this {@code Cell}'s {@link DistalDendrite}s
     */
    public List<DistalDendrite> getSegments(Connections c) {
        return c.getSegments(this);
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Cell: col=" + parentColumn.getIndex() + ", idx=" + index;
    }

    /**
     * {@inheritDoc}
     * 
     * <em> Note: All comparisons use the cell's index only </em>
     */
    @Override
    public int compareTo(Cell arg0) {
        return boxedIndex.compareTo(arg0.boxedIndex);
    }
}
