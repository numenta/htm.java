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
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.Connections.Activity;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.GroupBy2;
import org.numenta.nupic.util.GroupBy2.Slot;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;

import chaschev.lang.Pair;

/**
 * Temporal Memory implementation in Java.
 * 
 * @author Numenta
 * @author cogmission
 */
public class TemporalMemory implements ComputeDecorator, Serializable{
    /** simple serial version id */
	private static final long serialVersionUID = 1L;
    
    private static final double EPSILON = 0.00001;
    
    private static final int ACTIVE_COLUMNS = 1;
    
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

	@Override
	public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn) {
	    ComputeCycle cycle = new ComputeCycle();
		activateCells(connections, cycle, activeColumns, learn);
		activateDendrites(connections, cycle, learn);
		
		return cycle;
	}
	
	/**
	 * Calculate the active cells, using the current active columns and dendrite
     * segments. Grow and reinforce synapses.
     * 
     * <pre>
     * Pseudocode:
     *   for each column
     *     if column is active and has active distal dendrite segments
     *       call activatePredictedColumn
     *     if column is active and doesn't have active distal dendrite segments
     *       call burstColumn
     *     if column is inactive and has matching distal dendrite segments
     *       call punishPredictedColumn
     *      
     * </pre>
     * 
	 * @param conn                     
	 * @param activeColumnIndices
	 * @param learn
	 */
	@SuppressWarnings("unchecked")
    public void activateCells(Connections conn, ComputeCycle cycle, int[] activeColumnIndices, boolean learn) {
	    
	    ColumnData columnData = new ColumnData();
        
        Set<Cell> prevActiveCells = conn.getActiveCells();
        Set<Cell> prevWinnerCells = conn.getWinnerCells();
        
        List<Column> activeColumns = Arrays.stream(activeColumnIndices)
            .sorted()
            .mapToObj(i -> conn.getColumn(i))
            .collect(Collectors.toList());
        
        Function<Column, Column> identity = Function.identity();
        Function<DistalDendrite, Column> segToCol = segment -> segment.getParentCell().getColumn(); 
        
        @SuppressWarnings({ "rawtypes" })
        GroupBy2<Column> grouper = GroupBy2.<Column>of(
            new Pair(activeColumns, identity),
            new Pair(new ArrayList<>(conn.getActiveSegments()), segToCol),
            new Pair(new ArrayList<>(conn.getMatchingSegments()), segToCol));
        
        double permanenceIncrement = conn.getPermanenceIncrement();
        double permanenceDecrement = conn.getPermanenceDecrement();
        
        for(Tuple t : grouper) {
            columnData = columnData.set(t);
            
            if(columnData.isNotNone(ACTIVE_COLUMNS)) {
                if(!columnData.activeSegments().isEmpty()) {
                    List<Cell> cellsToAdd = activatePredictedColumn(conn, columnData.activeSegments(),
                        columnData.matchingSegments(), prevActiveCells, prevWinnerCells, 
                            permanenceIncrement, permanenceDecrement, learn);
                    
                    cycle.activeCells.addAll(cellsToAdd);
                    cycle.winnerCells.addAll(cellsToAdd);
                }else{
                    Tuple cellsXwinnerCell = burstColumn(conn, columnData.column(), columnData.matchingSegments(), 
                        prevActiveCells, prevWinnerCells, permanenceIncrement, permanenceDecrement, conn.getRandom(), 
                           learn);
                    
                    cycle.activeCells.addAll((List<Cell>)cellsXwinnerCell.get(0));
                    cycle.winnerCells.add((Cell)cellsXwinnerCell.get(1));
                }
            }else{
                if(learn) {
                    punishPredictedColumn(conn, columnData.activeSegments(), columnData.matchingSegments(), 
                        prevActiveCells, prevWinnerCells, conn.getPredictedSegmentDecrement());
                }
            }
        }
	}
	
	/**
	 * Calculate dendrite segment activity, using the current active cells.
	 * 
	 * <pre>
	 * Pseudocode:
     *   for each distal dendrite segment with activity >= activationThreshold
     *     mark the segment as active
     *   for each distal dendrite segment with unconnected activity >= minThreshold
     *     mark the segment as matching
     * </pre>
     * 
	 * @param conn     the Connectivity
	 * @param cycle    Stores current compute cycle results
	 * @param learn    If true, segment activations will be recorded. This information is used
     *                 during segment cleanup.
	 */
	public void activateDendrites(Connections conn, ComputeCycle cycle, boolean learn) {
	    Activity activity = conn.computeActivity(cycle.activeCells, conn.getConnectedPermanence());
	    
	    List<DistalDendrite> activeSegments = IntStream.range(0, activity.numActiveConnected.length)
	        .filter(i -> activity.numActiveConnected[i] >= conn.getActivationThreshold())
	        .mapToObj(i -> conn.segmentForFlatIdx(i))
	        .collect(Collectors.toList());
	    
	    List<DistalDendrite> matchingSegments = IntStream.range(0, activity.numActiveConnected.length)
	        .filter(i -> activity.numActivePotential[i] >= conn.getMinThreshold())
	        .mapToObj(i -> conn.segmentForFlatIdx(i))
	        .collect(Collectors.toList());
	    
	    Collections.sort(activeSegments, conn.segmentPositionSortKey);
	    Collections.sort(matchingSegments, conn.segmentPositionSortKey);
	    
	    cycle.activeSegments = activeSegments;
	    cycle.matchingSegments = matchingSegments;
	    
	    conn.lastActivity = activity;
	    conn.setActiveCells(new LinkedHashSet<>(cycle.activeCells));
        conn.setWinnerCells(new LinkedHashSet<>(cycle.winnerCells));
        conn.setActiveSegments(activeSegments);
        conn.setMatchingSegments(matchingSegments);
        // Forces generation of the predictive cells from the above active segments
        conn.clearPredictiveCells();
        conn.getPredictiveCells();
	    
	    if(learn) {
	        activeSegments.stream().forEach(s -> conn.recordSegmentActivity(s));
	        conn.startNewIteration();
	    }
	}
	
	/**
     * Indicates the start of a new sequence. Clears any predictions and makes sure
     * synapses don't grow to the currently active cells in the next time step.
     */
    @Override
    public void reset(Connections connections) {
        connections.getActiveCells().clear();
        connections.getWinnerCells().clear();
        connections.getActiveSegments().clear();
        connections.getMatchingSegments().clear();
    }
    
    /**
	 * Determines which cells in a predicted column should be added to winner cells
     * list, and learns on the segments that correctly predicted this column.
     * 
	 * @param conn                 the connections
	 * @param activeSegments       Active segments in the specified column
	 * @param matchingSegments     Matching segments in the specified column
	 * @param prevActiveCells      Active cells in `t-1`
	 * @param prevWinnerCells      Winner cells in `t-1`
	 * @param learn                If true, grow and reinforce synapses
	 * 
	 * <pre>
	 * Pseudocode:
     *   for each cell in the column that has an active distal dendrite segment
     *     mark the cell as active
     *     mark the cell as a winner cell
     *     (learning) for each active distal dendrite segment
     *       strengthen active synapses
     *       weaken inactive synapses
     *       grow synapses to previous winner cells
     * </pre>
     * 
	 * @return A list of predicted cells that will be added to active cells and winner
     *         cells.
	 */
	public List<Cell> activatePredictedColumn(Connections conn, List<DistalDendrite> activeSegments,
	    List<DistalDendrite> matchingSegments, Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells,
	        double permanenceIncrement, double permanenceDecrement, boolean learn) {
	    
	    List<Cell> cellsToAdd = new ArrayList<>();
        Cell previousCell = null;
        Cell currCell;
        for(DistalDendrite segment : activeSegments) {
            if((currCell = segment.getParentCell()) != previousCell) {
                cellsToAdd.add(currCell);
                previousCell = currCell;
            }
            
            if(learn) {
                adaptSegment(conn, segment, prevActiveCells, permanenceIncrement, permanenceDecrement);
                
                int numActive = conn.getLastActivity().numActivePotential[segment.getIndex()];
                int nGrowDesired = conn.getMaxNewSynapseCount() - numActive;
                
                if(nGrowDesired > 0) {
                    growSynapses(conn, prevWinnerCells, segment, conn.getInitialPermanence(),
                        nGrowDesired, conn.getRandom());
                }
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
     * @param conn                      Connections instance for the TM
     * @param column                    Bursting {@link Column}
     * @param matchingSegments          List of matching {@link DistalDendrite}s
     * @param prevActiveCells           Active cells in `t-1`
     * @param prevWinnerCells           Winner cells in `t-1`
     * @param permanenceIncrement       Amount by which permanences of synapses
     *                                  are decremented during learning
     * @param permanenceDecrement       Amount by which permanences of synapses
     *                                  are incremented during learning
     * @param random                    Random number generator
     * @param learn                     Whether or not learning is enabled
     * 
     * @return  Tuple containing:
     *                  cells       list of the processed column's cells
     *                  bestCell    the best cell
     */
    public Tuple burstColumn(Connections conn, Column column, List<DistalDendrite> matchingSegments, 
        Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells, double permanenceIncrement, double permanenceDecrement, 
            Random random, boolean learn) {
        
        List<Cell> cells = column.getCells();
        Cell bestCell = null;
        
        if(!matchingSegments.isEmpty()) {
            int[] numPoten = conn.getLastActivity().numActivePotential;
            Comparator<DistalDendrite> cmp = (dd1,dd2) -> numPoten[dd1.getIndex()] - numPoten[dd2.getIndex()]; 
            
            DistalDendrite bestSegment = matchingSegments.stream().max(cmp).get();
            bestCell = bestSegment.getParentCell();
            
            if(learn) {
                adaptSegment(conn, bestSegment, prevActiveCells, permanenceIncrement, permanenceDecrement);
                
                int nGrowDesired = conn.getMaxNewSynapseCount() - numPoten[bestSegment.getIndex()];
                
                if(nGrowDesired > 0) {
                    growSynapses(conn, prevWinnerCells, bestSegment, conn.getInitialPermanence(), 
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
     * @param activeSegments                    An iterable of {@link DistalDendrite} actives
     * @param matchingSegments                  An iterable of {@link DistalDendrite} matching
     *                                          for the column compute is operating on
     *                                          that are matching; None if empty
     * @param prevActiveCells                   Active cells in `t-1`
     * @param prevWinnerCells                   Winner cells in `t-1`
     *                                          are decremented during learning.
     * @param predictedSegmentDecrement         Amount by which segments are punished for incorrect predictions
     */
    public void punishPredictedColumn(Connections conn, List<DistalDendrite> activeSegments, 
        List<DistalDendrite> matchingSegments, Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells,
           double predictedSegmentDecrement) {
        
        if(predictedSegmentDecrement > 0) {
            for(DistalDendrite segment : matchingSegments) {
                adaptSegment(conn, segment, prevActiveCells, -conn.getPredictedSegmentDecrement(), 0);
            }
        }
    }
	
	
    ////////////////////////////
    //     Helper Methods     //
    ////////////////////////////
    
	/**
     * Gets the cell with the smallest number of segments.
     * Break ties randomly.
     * 
     * @param conn      Connections instance for the tm
     * @param cells     List of {@link Cell}s
     * @param random    Random Number Generator
     * 
     * @return  the least used {@code Cell}
     */
    public Cell leastUsedCell(Connections conn, List<Cell> cells, Random random) {
        List<Cell> leastUsedCells = new ArrayList<>();
        int minNumSegments = Integer.MAX_VALUE;
        for(Cell cell : cells) {
            int numSegments = conn.numSegments(cell);
            
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
        
        for(Synapse synapse : conn.getSynapses(segment)) {
            Cell presynapticCell = synapse.getPresynapticCell();
            int index = candidates.indexOf(presynapticCell);
            if(index != -1) {
                candidates.remove(index);
            }
        }
        
        int candidatesLength = candidates.size();
        int nActual = nDesiredNewSynapses < candidatesLength ? nDesiredNewSynapses : candidatesLength;
        
        for(int i = 0;i < nActual;i++) {
            int rand = random.nextInt(candidates.size());
            conn.createSynapse(segment, candidates.get(rand), initialPermanence);
            candidates.remove(rand);
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
        
        // Destroying a synapse modifies the set that we're iterating through.
        List<Synapse> synapsesToDestroy = new ArrayList<>();
        
        for(Synapse synapse : conn.getSynapses(segment)) {
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
                synapsesToDestroy.add(synapse);
            }else{
                synapse.setPermanence(conn, permanence);
            }
        }
        
        for(Synapse s : synapsesToDestroy) {
            conn.destroySynapse(s);
        }
        
        if(conn.numSynapses(segment) == 0) {
            conn.destroySegment(segment);
        }
    }
    
	/**
     * Used in the {@link TemporalMemory#compute(Connections, int[], boolean)} method
     * to make pulling values out of the {@link GroupBy2} more readable and named.
     */
    @SuppressWarnings("unchecked")
    public static class ColumnData implements Serializable {
        /** Default Serial */
        private static final long serialVersionUID = 1L;
        Tuple t;
        
        public ColumnData() {}
        
        public ColumnData(Tuple t) {
            this.t = t;
        }
        
        public Column column() { return (Column)t.get(0); }
        public List<Column> activeColumns() { return (List<Column>)t.get(1); }
        public List<DistalDendrite> activeSegments() { 
            return ((List<?>)t.get(2)).get(0).equals(Slot.empty()) ? 
                Collections.emptyList() :
                    (List<DistalDendrite>)t.get(2); 
        }
        public List<DistalDendrite> matchingSegments() {
            return ((List<?>)t.get(3)).get(0).equals(Slot.empty()) ? 
                Collections.emptyList() :
                    (List<DistalDendrite>)t.get(3); 
        }
        
        public ColumnData set(Tuple t) { this.t = t; return this; }
        
        /**
         * Returns a boolean flag indicating whether the slot contained by the
         * tuple at the specified index is filled with the special empty
         * indicator.
         * 
         * @param memberIndex   the index of the tuple to assess.
         * @return  true if <em><b>not</b></em> none, false if it <em><b>is none</b></em>.
         */
        public boolean isNotNone(int memberIndex) {
            return !((List<?>)t.get(memberIndex)).get(0).equals(NONE);
        }
    } 
}
