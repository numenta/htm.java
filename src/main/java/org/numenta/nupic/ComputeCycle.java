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

import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;

/**
 * Contains a snapshot of the state attained during one computational
 * call to the {@link TemporalMemory}. The {@code TemporalMemory} uses
 * data from previous compute cycles to derive new data for the current cycle
 * through a comparison between states of those different cycles, therefore
 * this state container is necessary.
 * 
 * @author David Ray
 */
public class ComputeCycle {
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
     * the specified {@link TemporalMemory}
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
     * {@link TemporalMemory#computePredictiveCells(Connections, ComputeCycle, Map)}
     * @return
     */
    public Set<DistalDendrite> matchingSegments() {
        return matchingSegments;
    }
    
    /**
     * Returns the Set of matching {@link Cell}s from
     * {@link TemporalMemory#computePredictiveCells(Connections, ComputeCycle, Map)}
     * @return
     */
    public Set<Cell> matchingCells() {
        return matchingCells;
    }
}
