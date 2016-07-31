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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.algorithms.OldTemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;

/**
 * Contains a snapshot of the state attained during one computational
 * call to the {@link OldTemporalMemory}. The {@code TemporalMemory} uses
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
    public Set<Cell> predictiveCells = new LinkedHashSet<>();
    public Set<Cell> predictedInactiveCells = new LinkedHashSet<>();
    public Set<Cell> matchingCells = new LinkedHashSet<>();
    public Set<Column> successfullyPredictedColumns = new LinkedHashSet<>();
    public Set<DistalDendrite> activeSegments = new LinkedHashSet<>();
    public Set<DistalDendrite> learningSegments = new LinkedHashSet<>();
    public Set<DistalDendrite> matchingSegments = new LinkedHashSet<>();
    
    
    /**
     * Constructs a new {@code ComputeCycle}
     */
    public ComputeCycle() {}
    
    /**
     * Constructs a new {@code ComputeCycle} initialized with
     * the connections relevant to the current calling {@link Thread} for
     * the specified {@link OldTemporalMemory}
     * 
     * @param   c       the current connections state of the TemporalMemory
     */
    public ComputeCycle(Connections c) {
        this.activeCells = new LinkedHashSet<>(c.getActiveCells());
        this.winnerCells = new LinkedHashSet<>(c.getWinnerCells());
        this.predictiveCells = new LinkedHashSet<>(c.getPredictiveCells());
        this.successfullyPredictedColumns = new LinkedHashSet<>(c.getSuccessfullyPredictedColumns());
        this.activeSegments = new LinkedHashSet<>(c.getActiveSegments());
        this.learningSegments = new LinkedHashSet<>(c.getLearningSegments());
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
     * Returns the {@link Set} of predictive cells.
     * @return
     */
    public Set<Cell> predictiveCells() {
        return predictiveCells;
    }
    
    /**
     * Returns the {@link Set} of columns successfully predicted from t - 1.
     * 
     * @return  the current {@link Set} of predicted columns
     */
    public Set<Column> successfullyPredictedColumns() {
        return successfullyPredictedColumns;
    }
    
    /**
     * Returns the Set of learning {@link DistalDendrite}s
     * @return
     */
    public Set<DistalDendrite> learningSegments() {
        return learningSegments;
    }
    
    /**
     * Returns the Set of active {@link DistalDendrite}s
     * @return
     */
    public Set<DistalDendrite> activeSegments() {
        return activeSegments;
    }
    
    /**
     * Returns a Set of predicted inactive cells.
     * @return
     */
    public Set<Cell> predictedInactiveCells() {
        return predictedInactiveCells;
    }
    
    /**
     * Returns the Set of matching {@link DistalDendrite}s from 
     * {@link OldTemporalMemory#computePredictiveCells(Connections, ComputeCycle, Map)}
     * @return
     */
    public Set<DistalDendrite> matchingSegments() {
        return matchingSegments;
    }
    
    /**
     * Returns the Set of matching {@link Cell}s from
     * {@link OldTemporalMemory#computePredictiveCells(Connections, ComputeCycle, Map)}
     * @return
     */
    public Set<Cell> matchingCells() {
        return matchingCells;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((activeCells == null) ? 0 : activeCells.hashCode());
        result = prime * result + ((activeSegments == null) ? 0 : activeSegments.hashCode());
        result = prime * result + ((learningSegments == null) ? 0 : learningSegments.hashCode());
        result = prime * result + ((matchingCells == null) ? 0 : matchingCells.hashCode());
        result = prime * result + ((matchingSegments == null) ? 0 : matchingSegments.hashCode());
        result = prime * result + ((predictedInactiveCells == null) ? 0 : predictedInactiveCells.hashCode());
        result = prime * result + ((predictiveCells == null) ? 0 : predictiveCells.hashCode());
        result = prime * result + ((successfullyPredictedColumns == null) ? 0 : successfullyPredictedColumns.hashCode());
        result = prime * result + ((winnerCells == null) ? 0 : winnerCells.hashCode());
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
        if(activeSegments == null) {
            if(other.activeSegments != null)
                return false;
        } else if(!activeSegments.equals(other.activeSegments))
            return false;
        if(learningSegments == null) {
            if(other.learningSegments != null)
                return false;
        } else if(!learningSegments.equals(other.learningSegments))
            return false;
        if(matchingCells == null) {
            if(other.matchingCells != null)
                return false;
        } else if(!matchingCells.equals(other.matchingCells))
            return false;
        if(matchingSegments == null) {
            if(other.matchingSegments != null)
                return false;
        } else if(!matchingSegments.equals(other.matchingSegments))
            return false;
        if(predictedInactiveCells == null) {
            if(other.predictedInactiveCells != null)
                return false;
        } else if(!predictedInactiveCells.equals(other.predictedInactiveCells))
            return false;
        if(predictiveCells == null) {
            if(other.predictiveCells != null)
                return false;
        } else if(!predictiveCells.equals(other.predictiveCells))
            return false;
        if(successfullyPredictedColumns == null) {
            if(other.successfullyPredictedColumns != null)
                return false;
        } else if(!successfullyPredictedColumns.equals(other.successfullyPredictedColumns))
            return false;
        if(winnerCells == null) {
            if(other.winnerCells != null)
                return false;
        } else if(!winnerCells.equals(other.winnerCells))
            return false;
        return true;
    }
}
