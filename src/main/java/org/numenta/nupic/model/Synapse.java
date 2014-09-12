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

import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Represents a connection with varying strength which when above 
 * a configured threshold represents a valid connection. This class
 * may hold state because its scope is limited to a given {@link Thread}'s
 * {@link Connections} object.
 * 
 * @author Chetan Surpur
 * @author David Ray
 * 
 * @see DistalDendrite
 * @see TemporalMemory.Connections
 */
public class Synapse {
    private Cell sourceCell;
    private Segment segment;
    private double permanence;
    private int index;
    /** The connection index of the input vector */
    private int inputIndex;
    
    
    /**
     * Constructor used when setting parameters later.
     */
    public Synapse() {}
    
    /**
     * Constructs a new {@code Synapse}
     * 
     * @param c             the connections state of the temporal memory
     * @param sourceCell    the {@link Cell} which will activate this {@code Synapse}
     * @param segment       the owning dendritic segment
     * @param permanence    this {@code Synapse}'s permanence
     * @param index         this {@code Synapse}'s index
     * @param inputIndex	this {@code Synapse}'s obverse input vector connection bit
     */
    public Synapse(Connections c, Cell sourceCell, Segment segment, double permanence, int index, int inputIndex) {
        this.sourceCell = sourceCell;
        this.segment = segment;
        this.permanence = permanence;
        this.index = index;
        this.inputIndex = inputIndex;
        
        // If this isn't a synapse on a proximal dendrite
        if(sourceCell != null) {
        	sourceCell.addReceptorSynapse(c, this);
        }
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
    public void setPermanence(double perm) {
        this.permanence = perm;
    }
    
    /**
     * Returns the owning dendritic segment
     * @return
     */
    public Segment getSegment() {
        return segment;
    }
    
    /**
     * Returns the index of the input bit this {@code Synapse} 
     * is connected to.
     * 
     * @return
     */
    public int getInputIndex() {
    	return inputIndex;
    }
    
    /**
     * Returns the containing {@link Cell} 
     * @return
     */
    public Cell getSourceCell() {
        return sourceCell;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "" + index;
    }
}
