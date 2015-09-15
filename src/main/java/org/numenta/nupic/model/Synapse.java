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

import org.numenta.nupic.Connections;

/**
 * Represents a connection with varying strength which when above 
 * a configured threshold represents a valid connection. 
 * 
 * IMPORTANT: 	For DistalDendrites, there is only one synapse per pool, so the
 * 				synapse's index doesn't really matter (in terms of tracking its
 * 				order within the pool). In that case, the index is a global counter
 * 				of all distal dendrite synapses.
 * 
 * 				For ProximalDendrites, there are many synapses within a pool, and in
 * 				that case, the index specifies the synapse's sequence order within
 * 				the pool object, and may be referenced by that index.
 *    
 * 
 * @author Chetan Surpur
 * @author David Ray
 * 
 * @see DistalDendrite
 * @see Connections
 */
public class Synapse {
    private Cell sourceCell;
    private Segment segment;
    private Pool pool;
    private int synapseIndex;
    private int inputIndex;
    private double permanence;

    
    /**
     * Constructor used when setting parameters later.
     */
    public Synapse() {}

    /**
     * Constructs a new {@code Synapse}
     * 
     * @param c             the connections state of the temporal memory
     * @param sourceCell    the {@link Cell} which will activate this {@code Synapse};
     *                      Null if this Synapse is proximal
     * @param segment       the owning dendritic segment
     * @param pool		    this {@link Pool} of which this synapse is a member
     * @param index         this {@code Synapse}'s index
     * @param inputIndex	the index of this {@link Synapse}'s input; be it a Cell or InputVector bit.
     */
    public Synapse(Connections c, Cell sourceCell, Segment segment, Pool pool, int index, int inputIndex) {
        this.sourceCell = sourceCell;
        this.segment = segment;
        this.pool = pool;
        this.synapseIndex = index;
        this.inputIndex = inputIndex;
        
        // If this isn't a synapse on a proximal dendrite
        if(sourceCell != null) {
            sourceCell.addReceptorSynapse(c, this);
        }
    }

    /**
     * Returns this {@code Synapse}'s index.
     * @return
     */
    public int getIndex() {
        return synapseIndex;
    }

    /**
     * Returns the index of this {@code Synapse}'s input item
     * whether it is a "sourceCell" or inputVector bit.
     * @return
     */
    public int getInputIndex() {
        return inputIndex;
    }

    /**
     * Returns this {@code Synapse}'s degree of connectedness.
     * @return
     */
    public double getPermanence() {
        return permanence;
    }

    /**
     * Sets this {@code Synapse}'s degree of connectedness.
     * @param perm
     */
    public void setPermanence(Connections c, double perm) {
        this.permanence = perm;
        if(sourceCell == null) {
            pool.updatePool(c, this, perm);
        }
    }

    /**
     * Returns the owning dendritic segment
     * @return
     */
    public Segment getSegment() {
        return segment;
    }

    /**
     * Returns the containing {@link Cell} 
     * @return
     */
    public Cell getPresynapticCell() {
        return sourceCell;
    }
    
    /**
     * Removes the references to this Synapse in its associated
     * {@link Pool} and its upstream presynapticCell's reference.
     * 
     * @param c
     */
    public void destroy(Connections c) {
        this.pool.destroySynapse(this);
        if(sourceCell != null) {
            c.getSynapses((DistalDendrite)segment).remove(this);
            sourceCell.removeReceptorSynapse(c, this);
        }else{
            c.getSynapses((ProximalDendrite)segment).remove(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("synapse: [ synIdx=").append(synapseIndex).append(", inIdx=")
            .append(inputIndex).append(", sgmtIdx=").append(segment.getIndex());
        if(sourceCell != null) {
            sb.append(", srcCellIdx=").append(sourceCell.getIndex());
        }
        sb.append(" ]");
        return sb.toString();
    }
}
