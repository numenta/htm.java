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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.numenta.nupic.algorithms.TemporalMemory;

/**
 * Contains a snapshot of the state attained during one computational
 * call to the {@link TemporalMemory}. The {@code TemporalMemory} uses
 * data from previous compute cycles to derive new data for the current cycle
 * through a comparison between states of those different cycles, therefore
 * this state container is necessary.
 * 
 * @author David Ray
 */
public class ComputeCycle implements Persistable {
    private static final long serialVersionUID = 1L;
    
    public Set<Cell> activeCells = new LinkedHashSet<>();
    public Set<Cell> winnerCells = new LinkedHashSet<>();
    public List<DistalDendrite> activeSegments = new ArrayList<>();
    public List<DistalDendrite> matchingSegments = new ArrayList<>();
    public Set<Cell> predictiveCells = new LinkedHashSet<>();
        
    
    /**
     * Constructs a new {@code ComputeCycle}
     */
    public ComputeCycle() {}
    
    /**
     * Constructs a new {@code ComputeCycle} initialized with
     * the connections relevant to the current calling {@link Thread} for
     * the specified {@link TemporalMemory}
     * 
     * @param   c       the current connections state of the TemporalMemory
     */
    public ComputeCycle(Connections c) {
        this.activeCells = new LinkedHashSet<>(c.activeCells);
        this.winnerCells = new LinkedHashSet<>(c.winnerCells);
        this.predictiveCells = new LinkedHashSet<>(c.predictiveCells);
        this.activeSegments = new ArrayList<>(c.activeSegments);
        this.matchingSegments = new ArrayList<>(c.matchingSegments);
    }
    
    /**
     * Returns the current {@link Set} of active cells
     * 
     * @return  the current {@link Set} of active cells
     */
    public Set<Cell> activeCells() {
        return activeCells;
    }
    
    /**
     * Returns the current {@link Set} of winner cells
     * 
     * @return  the current {@link Set} of winner cells
     */
    public Set<Cell> winnerCells() {
        return winnerCells;
    }
    
    /**
     * Returns the {@link List} of sorted predictive cells.
     * @return
     */
    public Set<Cell> predictiveCells() {
        if(predictiveCells.isEmpty()) { 
            Cell previousCell = null;
            Cell currCell = null;
            
            for(DistalDendrite activeSegment : activeSegments) {
                if((currCell = activeSegment.getParentCell()) != previousCell) {
                    predictiveCells.add(previousCell = currCell);
                }
            }
        }
        return predictiveCells;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activeCells == null) ? 0 : activeCells.hashCode());
        result = prime * result + ((predictiveCells == null) ? 0 : predictiveCells.hashCode());
        result = prime * result + ((winnerCells == null) ? 0 : winnerCells.hashCode());
        result = prime * result + ((activeSegments == null) ? 0 : activeSegments.hashCode());
        result = prime * result + ((matchingSegments == null) ? 0 : matchingSegments.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        ComputeCycle other = (ComputeCycle)obj;
        if(activeCells == null) {
            if(other.activeCells != null)
                return false;
        } else if(!activeCells.equals(other.activeCells))
            return false;
        if(predictiveCells == null) {
            if(other.predictiveCells != null)
                return false;
        } else if(!predictiveCells.equals(other.predictiveCells))
            return false;
        if(winnerCells == null) {
            if(other.winnerCells != null)
                return false;
        } else if(!winnerCells.equals(other.winnerCells))
            return false;
        if(activeSegments == null) {
            if(other.activeSegments != null)
                return false;
        } else if(!activeSegments.equals(other.activeSegments))
            return false;
        if(matchingSegments == null) {
            if(other.matchingSegments != null)
                return false;
        } else if(!matchingSegments.equals(other.matchingSegments))
            return false;
        return true;
    }
}
