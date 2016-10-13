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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a proximal or distal dendritic segment. Segments are owned by
 * {@link Cell}s and in turn own {@link Synapse}s which are obversely connected
 * to by a "source cell", which is the {@link Cell} which will activate a given
 * {@link Synapse} owned by this {@code Segment}.
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class DistalDendrite extends Segment implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private Cell cell;
    
    private long lastUsedIteration;
    
    public int ordinal = -1;
    
    /**
     * Constructs a new {@code Segment} object with the specified owner
     * {@link Cell} and the specified index.
     * 
     * @param cell      the owner
     * @param flatIdx     this {@code Segment}'s index.
     */
    public DistalDendrite(Cell cell, int flatIdx, long lastUsedIteration, int ordinal) {
        super(flatIdx);
        
        this.cell = cell;
        this.ordinal = ordinal;
        this.lastUsedIteration = lastUsedIteration;
    }

    /**
     * Returns the owner {@link Cell}
     * 
     * @return
     */
    public Cell getParentCell() {
        return cell;
    }
    
    /**
     * Returns all {@link Synapse}s
     * 
     * @param c     the connections state of the temporal memory
     * @return
     */
    public List<Synapse> getAllSynapses(Connections c) {
        return c.getSynapses(this);
    }

    /**
     * Returns the synapses on a segment that are active due to lateral input
     * from active cells.
     * 
     * @param c                 the layer connectivity
     * @param activeCells       the active cells
     * @return  Set of {@link Synapse}s connected to active presynaptic cells.
     */
    public Set<Synapse> getActiveSynapses(Connections c, Set<Cell> activeCells) {
        Set<Synapse> synapses = new LinkedHashSet<>();
        
        for(Synapse synapse : c.getSynapses(this)) {
            if(activeCells.contains(synapse.getPresynapticCell())) {
                synapses.add(synapse);
            }
        }
        
        return synapses;
    }

    /**
     * Sets the last iteration in which this segment was active.
     * @param iteration
     */
    public void setLastUsedIteration(long iteration) {
        this.lastUsedIteration = iteration;
    }
    
    /**
     * Returns the iteration in which this segment was last active.
     * @return  the iteration in which this segment was last active.
     */
    public long lastUsedIteration() {
        return lastUsedIteration;
    }
    
    /**
     * Returns this {@code DistalDendrite} segment's ordinal
     * @return	this segment's ordinal
     */
    public int getOrdinal() {
		return ordinal;
	}

    /**
     * Sets the ordinal value (used for age determination) on this segment.
     * @param ordinal	the age or order of this segment
     */
	public void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.valueOf(index);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((cell == null) ? 0 : cell.hashCode());
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
        DistalDendrite other = (DistalDendrite)obj;
        if(cell == null) {
            if(other.cell != null)
                return false;
        } else if(!cell.equals(other.cell))
            return false;
        return true;
    }
    
    
  
}
