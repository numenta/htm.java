/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.algorithms;

import static org.numenta.nupic.util.GroupBy2.Slot.NONE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.ComputeCycle.ColumnData;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Connections.Activity;
import org.numenta.nupic.Connections.SegmentOverlap;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.GroupBy2;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;

import javafx.util.Pair;

public class TemporalMemory implements ComputeDecorator, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final double EPSILON = 0.00001;
    
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
    
    
    public static void init(Connections c) {
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

    /**
     * Feeds input record through TM, performing inference and learning.
     * 
     * @param   conn                    The column/cell structure and connectivity
     * @param   activeColumnIndices     Indexes of Columns active during the current cycle
     * @param   learn                   Whether or not learning is enabled
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ComputeCycle compute(Connections conn, int[] activeColumnIndices, boolean learn) {
        ComputeCycle cycle = new ComputeCycle();
        
        Set<Cell> prevActiveCells = conn.getActiveCells();
        Set<Cell> prevWinnerCells = conn.getWinnerCells();
        
        List<Column> activeColumns = Arrays.stream(activeColumnIndices)
            .sorted()
            .mapToObj(i -> conn.getColumn(i))
            .collect(Collectors.toList());
        
        Function<Column, Column> identity = Function.identity();
        Function<SegmentOverlap, Column> segToCol = segment -> segment.segment.getParentCell().getColumn(); 
        
        GroupBy2<Column> grouper = GroupBy2.<Column>of(
            new Pair(activeColumns, identity),
            new Pair(new ArrayList(conn.getActiveSegmentOverlaps()), segToCol),
            new Pair(new ArrayList(conn.getMatchingSegmentOverlaps()), segToCol));
        
        double permanenceIncrement = conn.getPermanenceIncrement();
        double permanenceDecrement = conn.getPermanenceDecrement();
        for(Tuple t : grouper) {
            ColumnData columnData = cycle.columnData.set(t);
            
            if(!((List<?>)t.get(1)).get(0).equals(NONE)) {
                if(!columnData.activeSegments().isEmpty()) {
                    List<Cell> cellsToAdd = activatePredictedColumn(conn, columnData.activeSegments(), 
                        prevActiveCells, conn.getPermanenceIncrement(), conn.getPermanenceDecrement(), learn);
                    
                    cycle.activeCells.addAll(cellsToAdd);
                    cycle.winnerCells.addAll(cellsToAdd);
                }else{
                    Tuple cellsXwinnerCell = burstColumn(conn, columnData.column(), columnData.matchingSegments(), prevActiveCells, prevWinnerCells,
                        permanenceIncrement, permanenceDecrement, conn.getRandom(), learn);
                    
                    cycle.activeCells.addAll((List<Cell>)cellsXwinnerCell.get(0));
                    cycle.winnerCells.add((Cell)cellsXwinnerCell.get(1));
                }
            }else{
                if(learn) {
                    punishPredictedColumn(conn, columnData.matchingSegments(), prevActiveCells, conn.getPredictedSegmentDecrement());
                }
            }
        }
            
        Activity activity = conn.computeActivity(cycle.activeCells, conn.getConnectedPermanence(), conn.getActivationThreshold(), 
            0.0, conn.getMinThreshold(), learn);
        
        cycle.activeSegOverlaps = activity.activeSegments;
        cycle.matchingSegOverlaps = activity.matchingSegments;
        
        conn.setActiveCells(new LinkedHashSet<>(cycle.activeCells));
        conn.setWinnerCells(new LinkedHashSet<>(cycle.winnerCells));
        conn.setActiveSegmentOverlaps(activity.activeSegments);
        conn.setMatchingSegmentOverlaps(activity.matchingSegments);
        // Forces generation of the predictive cells from the above active segments
        conn.clearPredictiveCells();
        conn.getPredictiveCells();
        
        return cycle;
    }
    
    /**
     * <p>
     * Determines which cells in a predicted column should be added to
     * winner cells list and calls adaptSegment on the segments that correctly
     * predicted this column.
     * </p><p>
     * <b>Pseudocode:</b>
     * </p><p>
     * <pre>
     * for each cell in the column that has an active distal dendrite segment
     *     mark the cell as active
     *     mark the cell as a winner cell
     *     (learning) for each active distal dendrite segment
     *         strengthen active synapses
     *         weaken inactive synapses
     * </pre>
     * </p>
     * 
     * @param conn                      {@link Connections} instance for the tm 
     * @param activeSegments            A iterable of SegmentOverlap objects for the
     *                                  column compute is operating on that are active
     * @param prevActiveCells           Active cells in `t-1`
     * @param permanenceIncrement       Amount by which permanences of synapses are
     *                                  incremented during learning.
     * @param permanenceDecrement       Amount by which permanences of synapses are
     *                                  decremented during learning.
     * @param learn
     * @return      A list of predicted cells that will be added to active cells and winner cells.
     */
    public List<Cell> activatePredictedColumn(Connections conn, List<SegmentOverlap> activeSegments,
        Set<Cell> prevActiveCells, double permanenceIncrement, double permanenceDecrement, boolean learn) {
        
        List<Cell> cellsToAdd = new ArrayList<>();
        Cell cell = null;
        for(SegmentOverlap active : activeSegments) {
            boolean newCell = cell != active.segment.getParentCell();
            if(newCell) {
                cell = active.segment.getParentCell();
                cellsToAdd.add(cell);
            }
            
            if(learn) {
                adaptSegment(conn, active.segment, prevActiveCells, permanenceIncrement, permanenceDecrement);
            }
        }
        
        return cellsToAdd;
    }
    
    /**
     * Activates all of the cells in an unpredicted active column,
     * chooses a winner cell, and, if learning is turned on, either adapts or
     * creates a segment. growSynapses is invoked on this segment.
     * </p><p>
     * <b>Pseudocode:</b>
     * </p><p>
     * <pre>
     *  mark all cells as active
     *  if there are any matching distal dendrite segments
     *      find the most active matching segment
     *      mark its cell as a winner cell
     *      (learning)
     *      grow and reinforce synapses to previous winner cells
     *  else
     *      find the cell with the least segments, mark it as a winner cell
     *      (learning)
     *      (optimization) if there are previous winner cells
     *          add a segment to this winner cell
     *          grow synapses to previous winner cells
     * </pre>
     * </p>
     * 
     * @param conn                      Connections instance for the tm
     * @param excitedColumn             Excited Column instance from 
     *                                  {@link #excitedColumnsGenerator(int[], List, List, Connections)}
     * @param prevActiveCells           Active cells in `t-1`
     * @param prevWinnerCells           Winner cells in `t-1`
     * @param initialPermanence         Initial permanence of a new synapse.
     * @param maxNewSynapseCount        The maximum number of synapses added to
                                        a segment during learning     
     * @param permanenceIncrement       Amount by which permanences of synapses
                                        are decremented during learning
     * @param permanenceDecrement       Amount by which permanences of synapses
                                        are incremented during learning
     * @param random                    Random number generator
     * 
     * @return  Tuple containing:
     *                  cells       list of the processed column's cells
     *                  bestCell    the best cell
     */
    public Tuple burstColumn(Connections conn, Column column, List<SegmentOverlap> matchingSegments, 
        Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells, double permanenceIncrement, double permanenceDecrement, 
            Random random, boolean learn) {
        
        List<Cell> cells = column.getCells();
        Cell bestCell = null;
        
        if(!matchingSegments.isEmpty()) {
            SegmentOverlap bestSegment = matchingSegments.stream().max((so1, so2) -> so1.overlap - so2.overlap).get();
            bestCell = bestSegment.segment.getParentCell();
            if(learn) {
                adaptSegment(conn, bestSegment.segment, prevActiveCells, permanenceIncrement, permanenceDecrement);
                
                int nGrowDesired = conn.getMaxNewSynapseCount() - bestSegment.overlap;
                
                if(nGrowDesired > 0) {
                    growSynapses(conn, prevWinnerCells, bestSegment.segment, conn.getInitialPermanence(), 
                        nGrowDesired, random); 
                }
            }
        }else{
            bestCell = leastUsedCell(conn, cells, random);
            if(learn) {
                int nGrowExact = Math.min(conn.getMaxNewSynapseCount(), prevWinnerCells.size());
                if(nGrowExact > 0) {
                    DistalDendrite bestSegment = conn.createSegment(bestCell);
                    growSynapses(conn, prevWinnerCells, bestSegment, conn.getInitialPermanence(), 
                        nGrowExact, random);
                }
            }
        }
        
        return new Tuple(cells, bestCell);
    }
    
    /**
     * Punishes the Segments that incorrectly predicted a column to be active.
     * 
     * <p>
     * <pre>
     * Pseudocode:
     *  for each matching segment in the column
     *    weaken active synapses
     * </pre>
     * </p>
     *   
     * @param conn                              Connections instance for the tm
     * @param excitedColumn                     Excited Column instance from excitedColumnsGenerator
     * @param prevActiveCells                   Active cells in `t-1`
     * @param predictedSegmentDecrement         Amount by which permanences of synapses
     *                                          are decremented during learning.
     */
    public void punishPredictedColumn(Connections conn, List<SegmentOverlap> matchingSegments, 
        Set<Cell> prevActiveCells, double predictedSegmentDecrement) {
        
        if(predictedSegmentDecrement > 0) {
            for(SegmentOverlap segment : matchingSegments) {
                adaptSegment(conn, segment.segment, prevActiveCells, -conn.getPredictedSegmentDecrement(), 0);
            }
        }
    }
    
    ////////////////////////////////
    //       Helper Functions     //
    ////////////////////////////////
    
    /**
     * Gets the cell with the smallest number of segments.
     * Break ties randomly.
     * 
     * @param conn      Connections instance for the tm
     * @param cells     List of {@link Cell}s
     * @param random    Random Number Generator
     * @param learn     Added learn to keep from creating data structures
     *                  for holding segments when learning is off.
     * 
     * @return  the least used {@code Cell}
     */
    public Cell leastUsedCell(Connections conn, List<Cell> cells, Random random) {
        List<Cell> leastUsedCells = new ArrayList<>();
        int minNumSegments = Integer.MAX_VALUE;
        for(Cell cell : cells) {
            int numSegments = conn.unDestroyedSegmentsForCell(cell).size();
            
            if(numSegments < minNumSegments) {
                minNumSegments = numSegments;
                leastUsedCells.clear();
            }
            
            if(numSegments == minNumSegments) {
                leastUsedCells.add(cell);
            }
        }
        
        int i = random.nextInt(leastUsedCells.size());
        return leastUsedCells.get(i);
    }
    
    /**
     * Creates nDesiredNewSynapes synapses on the segment passed in if
     * possible, choosing random cells from the previous winner cells that are
     * not already on the segment.
     * <p>
     * <b>Notes:</b> The process of writing the last value into the index in the array
     * that was most recently changed is to ensure the same results that we get
     * in the c++ implementation using iter_swap with vectors.
     * </p>
     * 
     * @param conn                      Connections instance for the tm
     * @param prevWinnerCells           Winner cells in `t-1`
     * @param segment                   Segment to grow synapses on.     
     * @param initialPermanence         Initial permanence of a new synapse.
     * @param nDesiredNewSynapses       Desired number of synapses to grow
     * @param random                    Tm object used to generate random
     *                                  numbers
     */
    public void growSynapses(Connections conn, Set<Cell> prevWinnerCells, DistalDendrite segment, 
        double initialPermanence, int nDesiredNewSynapses, Random random) {
        
        List<Cell> candidates = new ArrayList<>(prevWinnerCells);
        Collections.sort(candidates);
        int eligibleEnd = candidates.size() - 1;
        
        for(Synapse synapse : conn.unDestroyedSynapsesForSegment(segment)) {
            Cell presynapticCell = synapse.getPresynapticCell();
            int index = candidates.subList(0, eligibleEnd + 1).indexOf(presynapticCell);
            if(index != -1) {
                candidates.set(index, candidates.get(eligibleEnd));
                eligibleEnd--;
            }
        }
        
        int candidatesLength = eligibleEnd + 1;
        int nActual = nDesiredNewSynapses < candidatesLength ? nDesiredNewSynapses : candidatesLength;
        
        for(int i = 0;i < nActual;i++) {
            int rand = random.nextInt(candidatesLength);
            conn.createSynapse(segment, candidates.get(rand), initialPermanence);
            candidates.set(rand, candidates.get(candidatesLength - 1));
            candidatesLength--;
        }
    }
    
    /**
     * Updates synapses on segment.
     * Strengthens active synapses; weakens inactive synapses.
     *  
     * @param conn                      {@link Connections} instance for the tm
     * @param segment                   {@link DistalDendrite} to adapt
     * @param prevActiveCells           Active {@link Cell}s in `t-1`
     * @param permanenceIncrement       Amount to increment active synapses    
     * @param permanenceDecrement       Amount to decrement inactive synapses
     */
    public void adaptSegment(Connections conn, DistalDendrite segment, Set<Cell> prevActiveCells, 
        double permanenceIncrement, double permanenceDecrement) {
        
        for(Synapse synapse : conn.unDestroyedSynapsesForSegment(segment)) {
            double permanence = synapse.getPermanence();
            
            if(prevActiveCells.contains(synapse.getPresynapticCell())) {
                permanence += permanenceIncrement;
            }else{
                permanence -= permanenceDecrement;
            }
            
            // Keep permanence within min/max bounds
            permanence = permanence < 0 ? 0 : permanence > 1.0 ? 1.0 : permanence;
            
            // Use this to examine issues caused by subtle floating point differences
            // be careful to set the scale (1 below) to the max significant digits right of the decimal point
            // between the permanenceIncrement and initialPermanence
            //
            // permanence = new BigDecimal(permanence).setScale(1, RoundingMode.HALF_UP).doubleValue(); 
            
            if(permanence < EPSILON) {
                conn.destroySynapse(synapse);
            }else{
                synapse.setPermanence(conn, permanence);
            }
        }
        
        if(conn.numSynapses(segment) == 0) {
            conn.destroySegment(segment);
        }
    }
    
    /**
     * Indicates the start of a new sequence and resets the sequence
     * state of the TM.
     * 
     * @param connections   The {@link Connections} object containing the state
     */
    @Override
    public void reset(Connections connections) {
        connections.getActiveCells().clear();
        connections.getWinnerCells().clear();
        connections.getActiveSegmentOverlaps().clear();
        connections.getMatchingSegmentOverlaps().clear();
    }

}
