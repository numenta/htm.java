package org.numenta.nupic.algorithms;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.SparseObjectMatrix;

/**
 * Temporal Memory implementation in Java
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class TemporalMemory implements ComputeDecorator {
    /**
     * Constructs a new {@code TemporalMemory}
     */
    public TemporalMemory() {}
    
    /**
     * Uses the specified {@link Connections} object to Build the structural 
     * anatomy needed by this {@code TemporalMemory} to implement its algorithms.
     * 
     * The connections object holds the {@link Column} and {@link Cell} infrastructure,
     * and is used by both the {@link SpatialPooler} and {@link TemporalMemory}. Either of
     * these can be used separately, and therefore this Connections object may have its
     * Columns and Cells initialized by either the init method of the SpatialPooler or the
     * init method of the TemporalMemory. We check for this so that complete initialization
     * of both Columns and Cells occurs, without either being redundant (initialized more than
     * once). However, {@link Cell}s only get created when initializing a TemporalMemory, because
     * they are not used by the SpatialPooler.
     * 
     * @param   c       {@link Connections} object
     */
    public void init(Connections c) {
        SparseObjectMatrix<Column> matrix = c.getMemory() == null ?
            new SparseObjectMatrix<Column>(c.getColumnDimensions()) :
                c.getMemory();
        c.setMemory(matrix);
        
        int numColumns = matrix.getMaxIndex() + 1;
        c.setNumColumns(numColumns);
        int cellsPerColumn = c.getCellsPerColumn();
        Cell[] cells = new Cell[numColumns * cellsPerColumn];
        
        //Used as flag to determine if Column objects have been created.
        Column colZero = matrix.getObject(0);
        for(int i = 0;i < numColumns;i++) {
            Column column = colZero == null ? 
                new Column(cellsPerColumn, i) : matrix.getObject(i);
            for(int j = 0;j < cellsPerColumn;j++) {
                cells[i * cellsPerColumn + j] = column.getCell(j);
            }
            //If columns have not been previously configured
            if(colZero == null) matrix.set(i, column);
        }
        //Only the TemporalMemory initializes cells so no need to test for redundancy
        c.setCells(cells);
    }
    
    /////////////////////////// CORE FUNCTIONS /////////////////////////////
    /**
     * Feeds input record through TM, performing inferencing and learning
     * 
     * @param connections       the connection memory
     * @param activeColumns     direct proximal dendrite input
     * @param learn             learning mode flag
     * @return                  {@link ComputeCycle} container for one cycle of inference values.
     */
    public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn) {
        ComputeCycle result = computeFn(connections, connections.getColumnSet(activeColumns), connections.getPredictiveCells(), 
            connections.getActiveSegments(), connections.getActiveCells(), connections.getWinnerCells(), connections.getMatchingSegments(), 
                connections.getMatchingCells(), learn);
        
        connections.setActiveCells(result.activeCells);
        connections.setWinnerCells(result.winnerCells);
        connections.setPredictiveCells(result.predictiveCells);
        connections.setSuccessfullyPredictedColumns(result.successfullyPredictedColumns);
        connections.setActiveSegments(result.activeSegments);
        connections.setLearningSegments(result.learningSegments);
        connections.setMatchingSegments(result.matchingSegments);
        connections.setMatchingCells(result.matchingCells);
        
        return result; 
    }
    
    /**
     * Functional version of {@link #compute(int[], boolean)}. 
     * This method is stateless and concurrency safe.
     * 
     * @param c                             {@link Connections} object containing state of memory members
     * @param activeColumns                 active {@link Column}s in t
     * @param prevPredictiveCells           cells predicting in t-1
     * @param prevActiveSegments            active {@link Segment}s in t-1
     * @param prevActiveCells               active {@link Cell}s in t-1
     * @param prevWinnerCells               winner {@link Cell}s in t-1
     * @param prevMatchingSegments          matching {@link Segment}s in t-1
     * @param prevMatchingCells             matching cells in t-1 
     * @param learn                         whether mode is "learning" mode
     * @return
     */
    public ComputeCycle computeFn(Connections c, Set<Column> activeColumns, Set<Cell> prevPredictiveCells, Set<DistalDendrite> prevActiveSegments,
        Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells, Set<DistalDendrite> prevMatchingSegments, Set<Cell> prevMatchingCells, boolean learn) {
        
        ComputeCycle cycle = new ComputeCycle();
        
        activateCorrectlyPredictiveCells(c, cycle, prevPredictiveCells, prevMatchingCells, activeColumns);
        
        burstColumns(cycle, c, activeColumns, cycle.successfullyPredictedColumns, prevActiveCells, prevWinnerCells);
        
        if(learn) {
            learnOnSegments(c, prevActiveSegments, cycle.learningSegments, prevActiveCells, 
                cycle.winnerCells, prevWinnerCells, cycle.predictedInactiveCells, prevMatchingSegments);
        }
        
        computePredictiveCells(c, cycle, cycle.activeCells);
        
        return cycle;
    }
    
    /**
     * Phase 1: Activate the correctly predictive cells
     * 
     * Pseudocode:
     *
     * - for each previous predictive cell
     *   - if in active column
     *     - mark it as active
     *     - mark it as winner cell
     *     - mark column as predicted
     *   - if not in active column
     *     - mark it as a predicted but inactive cell
     *     
     * @param cnx                   Connectivity of layer
     * @param c                     ComputeCycle interim values container
     * @param prevPredictiveCells   predictive {@link Cell}s predictive cells in t-1
     * @param activeColumns         active columns in t
     */
    public void activateCorrectlyPredictiveCells(Connections cnx, ComputeCycle c, 
        Set<Cell> prevPredictiveCells, Set<Cell> prevMatchingCells, Set<Column> activeColumns) {
        
        for(Cell cell : prevPredictiveCells) {
            Column column = cell.getColumn();
            if(activeColumns.contains(column)) {
                c.activeCells.add(cell);
                c.winnerCells.add(cell);
                c.successfullyPredictedColumns.add(column);
            }
        }
        
        if(cnx.getPredictedSegmentDecrement() > 0) {
            for(Cell cell : prevMatchingCells) {
                Column column = cell.getColumn();
                
                if(!activeColumns.contains(column)) {
                    c.predictedInactiveCells.add(cell);
                }
            }
        }
    }
    
    /**
     * Phase 2: Burst unpredicted columns.
     * 
     * Pseudocode:
     *
     * - for each unpredicted active column
     *   - mark all cells as active
     *   - mark the best matching cell as winner cell
     *     - (learning)
     *       - if it has no matching segment
     *         - (optimization) if there are previous winner cells
     *           - add a segment to it
     *       - mark the segment as learning
     * 
     * @param cycle                         ComputeCycle interim values container
     * @param c                             Connections temporal memory state
     * @param activeColumns                 active columns in t
     * @param predictedColumns              predicted columns in t
     * @param prevActiveCells               active {@link Cell}s in t-1
     * @param prevWinnerCells               winner {@link Cell}s in t-1
     */
    public void burstColumns(ComputeCycle cycle, Connections c, 
        Set<Column> activeColumns, Set<Column> predictedColumns, Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells) {
        
        // Now contains only unpredicted columns
        activeColumns.removeAll(predictedColumns);
        
        for(Column column : activeColumns) {
            List<Cell> cells = column.getCells();
            cycle.activeCells.addAll(cells);
            
            CellSearch cellSearch = getBestMatchingCell(c, cells, prevActiveCells);
            
            cycle.winnerCells.add(cellSearch.bestCell);
            
            DistalDendrite bestSegment = cellSearch.bestSegment;
            if(bestSegment == null && prevWinnerCells.size() > 0) {
                bestSegment = cellSearch.bestCell.createSegment(c);
            }
            
            if(bestSegment != null) {
                cycle.learningSegments.add(bestSegment);
            }
        }
    }
    
    /**
     * Phase 3: Perform learning by adapting segments.
     * <pre>
     * Pseudocode:
     *
     * - (learning) for each previously active or learning segment
     *   - if learning segment or from winner cell
     *     - strengthen active synapses
     *     - weaken inactive synapses
     *   - if learning segment
     *     - add some synapses to the segment
     *       - sub sample from previous winner cells
     *   
     *   - if predictedSegmentDecrement > 0
     *     - for each previously matching segment
     *       - weaken active synapses but don't touch inactive synapses
     * </pre>    
     *     
     * @param c                             the Connections state of the temporal memory
     * @param prevActiveSegments            the Set of segments active in the previous cycle. "t-1"
     * @param learningSegments              the Set of segments marked as learning segments in "t"
     * @param prevActiveCells               the Set of active cells in "t-1"
     * @param winnerCells                   the Set of winner cells in "t"
     * @param prevWinnerCells               the Set of winner cells in "t-1"
     * @param predictedInactiveCells        the Set of predicted inactive cells
     * @param prevMatchingSegments          the Set of segments with
     * 
     */
    public void learnOnSegments(Connections c, Set<DistalDendrite> prevActiveSegments, 
        Set<DistalDendrite> learningSegments, Set<Cell> prevActiveCells, Set<Cell> winnerCells, Set<Cell> prevWinnerCells, 
            Set<Cell> predictedInactiveCells, Set<DistalDendrite> prevMatchingSegments) {
        
        double permanenceIncrement = c.getPermanenceIncrement();
        double permanenceDecrement = c.getPermanenceDecrement();
            
        Set<DistalDendrite> prevAndLearning = new HashSet<DistalDendrite>(prevActiveSegments);
        prevAndLearning.addAll(learningSegments);
        
        for(DistalDendrite dd : prevAndLearning) {
                
            boolean isLearningSegment = learningSegments.contains(dd);
            boolean isFromWinnerCell = winnerCells.contains(dd.getParentCell());
            
            Set<Synapse> activeSynapses = dd.getActiveSynapses(c, prevActiveCells);
            
            if(isLearningSegment || isFromWinnerCell) {
                dd.adaptSegment(c, activeSynapses, permanenceIncrement, permanenceDecrement);
            }
            
            int n = c.getMaxNewSynapseCount() - activeSynapses.size();
            if(isLearningSegment && n > 0) {
                Set<Cell> learnCells = dd.pickCellsToLearnOn(c, n, prevWinnerCells, c.getRandom());
                for(Cell sourceCell : learnCells) {
                    dd.createSynapse(c, sourceCell, c.getInitialPermanence());
                }
            }
        }
        
        if(c.getPredictedSegmentDecrement() > 0) {
            for(DistalDendrite segment : prevMatchingSegments) {
                boolean isPredictedInactiveCell = predictedInactiveCells.contains(segment.getParentCell());
                
                if(isPredictedInactiveCell) {
                    Set<Synapse> activeSynapses = segment.getActiveSynapses(c, prevActiveCells);
                    segment.adaptSegment(c, activeSynapses, -c.getPredictedSegmentDecrement(), 0.0);
                }
            }
        }
    }
    
    /**
     * Phase 4: Compute predictive cells due to lateral input on distal dendrites.
     *
     * Pseudocode:
     *
     * - for each distal dendrite segment with activity >= activationThreshold
     *   - mark the segment as active
     *   - mark the cell as predictive
     *   
     * - if predictedSegmentDecrement > 0
     *   - for each distal dendrite segment with unconnected activity > = minThreshold
     *     - mark the segment as matching
     *     - mark the cell as matching
     * 
     * @param c                 the Connections state of the temporal memory
     * @param cycle             the state during the current compute cycle
     * @param activeCells       the active {@link Cell}s in t
     */
    public void computePredictiveCells(Connections c, ComputeCycle cycle, Set<Cell> activeCells) {
        TObjectIntMap<DistalDendrite> numActiveConnectedSynapsesForSegment = new TObjectIntHashMap<>();
        TObjectIntMap<DistalDendrite> numActiveSynapsesForSegment = new TObjectIntHashMap<>();
        
        for(Cell cell : activeCells) {
            for(Synapse syn : c.getReceptorSynapses(cell)) {
                DistalDendrite segment = (DistalDendrite)syn.getSegment();
                double permanence = syn.getPermanence();
                
                if(permanence >= c.getConnectedPermanence()) {
                    numActiveConnectedSynapsesForSegment.adjustOrPutValue(segment, 1, 1);    
                    
                    if(numActiveConnectedSynapsesForSegment.get(segment) >= c.getActivationThreshold()) {
                        cycle.activeSegments.add(segment);
                        cycle.predictiveCells.add(segment.getParentCell());
                    }
                }
                
                if(permanence > 0 && c.getPredictedSegmentDecrement() > 0) {
                    numActiveSynapsesForSegment.adjustOrPutValue(segment, 1, 1);    
                    
                    if(numActiveSynapsesForSegment.get(segment) >= c.getMinThreshold()) {
                        cycle.matchingSegments.add(segment);
                        cycle.matchingCells.add(segment.getParentCell());
                    }
                }
            }
        }
    }
    
    /**
     * Called to start the input of a new sequence, and
     * reset the sequence state of the TM.
     * 
     * @param   connections   the Connections state of the temporal memory
     */
    public void reset(Connections connections) {
        connections.getActiveCells().clear();
        connections.getPredictiveCells().clear();
        connections.getActiveSegments().clear();
        connections.getWinnerCells().clear();
        connections.getMatchingCells().clear();
        connections.getMatchingSegments().clear();
    }
    
    /////////////////////////////////////////////////////////////
    //                    Helper functions                     //
    /////////////////////////////////////////////////////////////
    /**
     * Gets the cell with the best matching segment
     * (see `TM.bestMatchingSegment`) that has the largest number of active
     * synapses of all best matching segments.
     *
     * If none were found, pick the least used cell (see `TM.leastUsedCell`).
     *  
     * @param c                 Connections temporal memory state
     * @param columnCells             
     * @param activeCells
     * @return a CellSearch (bestCell, BestSegment)
     */
    public CellSearch getBestMatchingCell(Connections c, List<Cell> columnCells, Set<Cell> activeCells) {
        int maxSynapses = 0;
        Cell bestCell = null;
        DistalDendrite bestSegment = null;
        
        for(Cell cell : columnCells) {
            SegmentSearch bestMatchResult = getBestMatchingSegment(c, cell, activeCells);
            
            if(bestMatchResult.bestSegment != null &&  bestMatchResult.numActiveSynapses > maxSynapses) {
                maxSynapses = bestMatchResult.numActiveSynapses;
                bestCell = cell;
                bestSegment = bestMatchResult.bestSegment;
            }
        }
        
        if(bestCell == null) {
            bestCell = getLeastUsedCell(c, columnCells);
        }
        
        return new CellSearch(bestCell, bestSegment);
    }
    
    /**
     * Gets the segment on a cell with the largest number of activate synapses,
     * including all synapses with non-zero permanences.
     * 
     * @param c
     * @param columnCell
     * @param activeCells
     * @return
     */
    public SegmentSearch getBestMatchingSegment(Connections c, Cell columnCell, Set<Cell> activeCells) {
        int maxSynapses = c.getMinThreshold();
        DistalDendrite bestSegment = null;
        int bestNumActiveSynapses = 0;
        int numActiveSynapses = 0;
        
        for(DistalDendrite segment : c.getSegments(columnCell)) {
            numActiveSynapses = 0;
            for(Synapse synapse : c.getSynapses(segment)) {
                if(activeCells.contains(synapse.getPresynapticCell()) && synapse.getPermanence() > 0) {
                    ++numActiveSynapses;
                }
            }
            
            if(numActiveSynapses >= maxSynapses) {
                maxSynapses = numActiveSynapses;
                bestSegment = segment;
                bestNumActiveSynapses = numActiveSynapses;
            }
        }
        
        return new SegmentSearch(bestSegment, bestNumActiveSynapses);
    }
    
    /**
     * Gets the cell with the smallest number of segments.
     * Break ties randomly.
     * 
     * @param c
     * @param columnCells
     * @return
     */
    public Cell getLeastUsedCell(Connections c, List<Cell> columnCells) {
        Set<Cell> leastUsedCells = new LinkedHashSet<>();
        int minNumSegments = Integer.MAX_VALUE;
        
        for(Cell cell : columnCells) {
            int numSegments = c.getSegments(cell).size();
            
            if(numSegments < minNumSegments) {
                minNumSegments = numSegments;
                leastUsedCells.clear();
            }
            
            if(numSegments == minNumSegments) {
                leastUsedCells.add(cell);
            }
        }
        
        int randomIdx = c.getRandom().nextInt(leastUsedCells.size());
        List<Cell> l = new ArrayList<>(leastUsedCells);
        Collections.sort(l);
        return l.get(randomIdx);
    }
    
    /**
     * Used locally to return results of column Burst
     */
    class BurstResult {
        Set<Cell> activeCells;
        Set<Cell> winnerCells;
        Set<DistalDendrite> learningSegments;
        
        public BurstResult(Set<Cell> activeCells, Set<Cell> winnerCells, Set<DistalDendrite> learningSegments) {
            super();
            this.activeCells = activeCells;
            this.winnerCells = winnerCells;
            this.learningSegments = learningSegments;
        }
    }
    
    /**
     * Used locally to return best cell/segment pair
     */
    class CellSearch {
        Cell bestCell;
        DistalDendrite bestSegment;
        public CellSearch(Cell bestCell, DistalDendrite bestSegment) {
            this.bestCell = bestCell;
            this.bestSegment = bestSegment;
        }
    }
    
    /**
     * Used locally to return best segment matching results
     */
    class SegmentSearch {
        DistalDendrite bestSegment;
        int numActiveSynapses;
        
        public SegmentSearch(DistalDendrite bestSegment, int numActiveSynapses) {
            this.bestSegment = bestSegment;
            this.numActiveSynapses = numActiveSynapses;
        }
    }
}
