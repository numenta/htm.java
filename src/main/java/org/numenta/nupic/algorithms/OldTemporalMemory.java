package org.numenta.nupic.algorithms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.Generator;
import org.numenta.nupic.util.IntGenerator;
import org.numenta.nupic.util.NamedTuple;
import org.numenta.nupic.util.SegmentGenerator;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;

public class OldTemporalMemory implements ComputeDecorator, Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
     * Constructs a new {@code TemporalMemory}
     */
	public OldTemporalMemory() {}
	
	/**
     * Uses the specified {@link Connections} object to Build the structural 
     * anatomy needed by this {@code TemporalMemory} to implement its algorithms.
     * 
     * The connections object holds the {@link Column} and {@link Cell} infrastructure,
     * and is used by both the {@link SpatialPooler} and {@link OldTemporalMemory}. Either of
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
    
    @SuppressWarnings("serial")
    private Generator<ExcitedColumn> excitedColumnsGenerator(
        int[] activeColumns, List<DistalDendrite> activeSegments, List<DistalDendrite> matchingSegments,
            Connections connections) {
        
        return new Generator<ExcitedColumn>() {
            int activeColumnsProcessed = 0;
            int activeSegmentsProcessed = 0;
            int matchingSegmentsProcessed = 0;

            int activeColumnsNum = activeColumns.length;
            int activeSegmentsNum = activeSegments.size();
            int matchingSegmentsNum = matchingSegments.size();
            
            boolean isActiveColumn;
            
            @Override
            public ExcitedColumn next() {
                int currentColumn = Integer.MAX_VALUE;
                if(activeSegmentsProcessed < activeSegmentsNum) {
                    currentColumn = Math.min(
                        currentColumn, 
                        connections.columnIndexForSegment(activeSegments.get(activeSegmentsProcessed)));
                }
                
                if(matchingSegmentsProcessed < matchingSegmentsNum) {
                    currentColumn = Math.min(
                        currentColumn,
                        connections.columnIndexForSegment(matchingSegments.get(matchingSegmentsProcessed)));
                }
                
                if(activeColumnsProcessed < activeColumnsNum &&
                    activeColumns[activeColumnsProcessed] <= currentColumn) {
                    
                    currentColumn = activeColumns[activeColumnsProcessed];
                    isActiveColumn = true;
                    activeColumnsProcessed += 1;
                } else {
                    isActiveColumn = false;
                }
                
                int activeSegmentsBegin = activeSegmentsProcessed;
                int activeSegmentsEnd = activeSegmentsProcessed;
                for(int i = activeSegmentsProcessed;i < activeSegmentsNum;i++) {
                    if(connections.columnIndexForSegment(activeSegments.get(i)) == currentColumn) {
                        activeSegmentsProcessed += 1;
                        activeSegmentsEnd += 1;
                    } else {
                        break;
                    }
                }
                
                int matchingSegmentsBegin = matchingSegmentsProcessed;
                int matchingSegmentsEnd = matchingSegmentsProcessed;
                for(int i = matchingSegmentsProcessed;i < matchingSegmentsNum;i++) {
                    if(connections.columnIndexForSegment(matchingSegments.get(i)) == currentColumn) {
                        matchingSegmentsProcessed += 1;
                        matchingSegmentsEnd += 1;
                    } else {
                        break;
                    }
                }
                
                Generator<Integer> asIndexGenerator = IntGenerator.of(activeSegmentsBegin, activeSegmentsEnd);
                Generator<Integer> msIndexGenerator = IntGenerator.of(matchingSegmentsBegin, matchingSegmentsEnd);
                return new ExcitedColumn(
                    currentColumn, 
                    isActiveColumn, 
                    SegmentGenerator.of(activeSegments, asIndexGenerator), 
                    activeSegmentsEnd - activeSegmentsBegin,
                    SegmentGenerator.of(matchingSegments, msIndexGenerator),
                    matchingSegmentsEnd - matchingSegmentsBegin
                );
            }

            @Override
            public boolean hasNext() {
                return (activeColumnsProcessed < activeColumnsNum ||
                        activeSegmentsProcessed < activeSegmentsNum ||
                        matchingSegmentsProcessed < matchingSegmentsNum);
            }
        };
    }

	/////////////////////////// CORE FUNCTIONS /////////////////////////////
    
    /**
     * Feeds input record through TM, performing inference and learning.
     * <p>
     * <b>Pseudocode:</b>
     * </p><p>
     * <pre>
     *  for each column
     *      if column is active and has active distal dendrite segments
     *          call activatePredictedColumn
     *      if column is active and doesn't have active distal dendrite segments
     *          call burstColumn
     *      if column is inactive and has matching distal dendrite segments
     *          call punishPredictedColumn
     *  for each distal dendrite segment with activity >= activationThreshold
     *      mark the segment as active
     *  for each distal dendrite segment with unconnected activity >= minThreshold
     *      mark the segment as matching
     *
     *  Updates {@link Connections} member variables:
     *    - `activeCells`     (set)
     *    - `winnerCells`     (set)
     *    - `activeSegments`  (set)
     *    - `matchingSegments`(set)
     * </pre>
     * </p>
     * 
     * @param activeColumns (set)  Indices of active columns
     * @param learn         (boolean) Whether or not learning is enabled
     */
    @SuppressWarnings("unchecked")
    @Override
    public ComputeCycle compute(Connections conn, int[] activeColumns, boolean learn) {
        ComputeCycle cycle = new ComputeCycle();
        
        Set<Cell> prevActiveCells = conn.getActiveCells();
        Set<Cell> prevWinnerCells = conn.getWinnerCells();
        
        Arrays.sort(activeColumns);
        
        for(ExcitedColumn excitedColumn : excitedColumnsGenerator(
            activeColumns, new ArrayList<>(conn.getActiveSegments()), 
                new ArrayList<>(conn.getMatchingSegments()), conn)) {
            
            if(excitedColumn.isActiveColumn) {
                if(excitedColumn.activeSegmentsCount != 0) {
                    List<Cell> cellsToAdd = activatePredictedColumn(
                        conn, excitedColumn, prevActiveCells, conn.getPermanenceIncrement(), 
                            conn.getPermanenceDecrement(), learn);
                    
                    cycle.activeCells.addAll(cellsToAdd);
                    cycle.winnerCells.addAll(cellsToAdd);
                } else {
                    Tuple bestCellxWinnerCell = burstColumn(
                        conn, excitedColumn, prevActiveCells, prevWinnerCells, conn.getMaxNewSynapseCount(),
                            conn.getInitialPermanence(), conn.getPermanenceIncrement(), 
                                conn.getPermanenceDecrement(), conn.getRandom(), learn);
                    
                    cycle.activeCells.addAll((List<Cell>)bestCellxWinnerCell.get(0));
                    cycle.winnerCells.add((Cell)bestCellxWinnerCell.get(1));
                }
            } else if(learn) {
                punishPredictedColumn(conn, excitedColumn, prevActiveCells, conn.getPredictedSegmentDecrement());
            }
        }
        
        // Tuple = [activeSegments, matchingSegments]
        Tuple activeMatchingSegments = conn.computeActivity(cycle.activeCells, conn.getConnectedPermanence(), 
            conn.getActivationThreshold(), 0.0, conn.getMinThreshold());
        
        cycle.activeSegments = (Set<DistalDendrite>)activeMatchingSegments.get(0);
        cycle.matchingSegments = (Set<DistalDendrite>)activeMatchingSegments.get(1);
        
        conn.setActiveCells(new LinkedHashSet<>(cycle.activeCells));
        conn.setWinnerCells(new LinkedHashSet<>(cycle.winnerCells));
        conn.setActiveSegments(new LinkedHashSet<>(cycle.activeSegments));
        conn.setMatchingSegments(new LinkedHashSet<>(cycle.matchingSegments));
        
        return cycle;
    }
    
    /**
     * Signals the start of a new sequence and resets the sequence
     * state of the TM.
     * 
     * @param   connections     {@link Connections} instance for the tm
     */
    @Override
    public void reset(Connections connections) {
        connections.getActiveCells().clear();
        connections.getWinnerCells().clear();
        connections.getActiveSegments().clear();
        connections.getMatchingSegments().clear();
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
     * @param excitedColumn             {@link NamedTuple} generated by 
     *                                  {@link #excitedColumnsGenerator(int[], List, List, Connections)}
     * @param prevActiveCells           Active cells in `t-1`
     * @param permanenceIncrement       Amount by which permanences of synapses are
     *                                  decremented during learning.
     * @param permanenceDecrement       Amount by which permanences of synapses are
     *                                  incremented during learning.
     * @param learn                     Determines if permanences are adjusted
     * 
     * @return cellsToAdd               A list of predicted cells that will be added to active cells 
     *                                  and winner cells.
     */
    public List<Cell> activatePredictedColumn(Connections conn, ExcitedColumn excitedColumn, 
        Set<Cell> prevActiveCells, double permanenceIncrement, double permanenceDecrement, boolean learn) {
        
        List<Cell> cellsToAdd = new ArrayList<>();
        Cell cell = null;
        for(DistalDendrite active : excitedColumn.activeSegments) {
            boolean newCell = cell != active.getParentCell();
            if(newCell) {
                cell = active.getParentCell();
                cellsToAdd.add(cell);
            }
            
            if(learn) {
                active.adaptSegment(prevActiveCells, conn, permanenceIncrement, permanenceDecrement);
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
    int count = 0;
    public Tuple burstColumn(Connections conn, ExcitedColumn excitedColumn, Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells,
        int maxNewSynapseCount, double initialPermanence, double permanenceIncrement, double permanenceDecrement, Random random, boolean learn) {
        
        List<Cell> cells = conn.getColumn(excitedColumn.column).getCells();
        Cell bestCell = null;
        
        if(excitedColumn.matchingSegmentsCount != 0) {
            // Tuple = [bestSegment, bestNumActiveSynapses]
            Tuple bestMatch = bestMatchingSegment(conn, excitedColumn, prevActiveCells);
            
            DistalDendrite bestSegment = (DistalDendrite)bestMatch.get(0);
            bestCell = bestSegment.getParentCell();
            
            if(learn) {
                bestSegment.adaptSegment(prevActiveCells, conn, permanenceIncrement, permanenceDecrement);
                
                int nGrowDesired = maxNewSynapseCount - (int)bestMatch.get(1);
                if(nGrowDesired > 0) {
                    growSynapses(conn, prevWinnerCells, bestSegment, initialPermanence, nGrowDesired, random);
                }
            }
        } else {
            bestCell = leastUsedCell(conn, cells, random, learn);
            if(learn) {
                int nGrowExact = Integer.min(maxNewSynapseCount, prevWinnerCells.size());
                if(nGrowExact > 0) {
                    DistalDendrite bestSegment = bestCell.createSegment(conn);
                    growSynapses(conn, prevWinnerCells, bestSegment, initialPermanence, nGrowExact, random);
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
    public void punishPredictedColumn(Connections conn, ExcitedColumn excitedColumn, 
        Set<Cell> prevActiveCells, double predictedSegmentDecrement) {
        
        if(predictedSegmentDecrement > 0) {
            for(DistalDendrite segment : excitedColumn.matchingSegments) {
                segment.adaptSegment(prevActiveCells, conn, -conn.getPredictedSegmentDecrement(), 0);
            }
        }
    }
    
    //////////////////////////////////
    //       Helper Functions       //
    //////////////////////////////////    
    /**
     * Gets the segment on a cell with the largest number of active synapses.
     * Returns an int representing the segment and the number of synapses
     * corresponding to it.
     * 
     * @param conn                  Connections instance for the tm
     * @param excitedColumn         Excited Column instance from
                                    excitedColumnsGenerator
     * @param prevActiveCells       Active cells in `t-1`
     * 
     * @return  Tuple containing:
     *              bestSegment             (DistalDendrite)
     *              bestNumActiveSynapses   (int)
     */
    public Tuple bestMatchingSegment(Connections conn, ExcitedColumn excitedColumn, Set<Cell> prevActiveCells) {
        int maxSynapses = 0;
        DistalDendrite bestSegment = null;
        int bestNumActiveSynapses = 0;
        
        for(DistalDendrite segment : excitedColumn.matchingSegments) {
            int numActiveSynapses = 0;
            
            for(Synapse syn : conn.getSynapses(segment)) {
                if(prevActiveCells.contains(syn.getPresynapticCell())) {
                    numActiveSynapses += 1;
                }
            }
            
            if(numActiveSynapses >= maxSynapses) {
                maxSynapses = numActiveSynapses;
                bestSegment = segment;
                bestNumActiveSynapses = numActiveSynapses;
            }
        }
        
        return new Tuple(bestSegment, bestNumActiveSynapses);
    }
    
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
    public Cell leastUsedCell(Connections conn, List<Cell> cells, Random random, boolean learn) {
        List<Cell> leastUsedCells = new ArrayList<>();
        int minNumSegments = Integer.MAX_VALUE;
        for(Cell cell : cells) {
            int numSegments = cell.getSegments(conn, learn).size();
            
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
            int eligibleEnd = candidates.size();
            
            for(Synapse synapse : conn.getSynapses(segment)) {
                Cell presynapticCell = synapse.getPresynapticCell();
                int ineligible = candidates.subList(0, eligibleEnd).indexOf(presynapticCell);
                if(ineligible != -1) {
                    eligibleEnd--;
                    candidates.set(ineligible, candidates.get(eligibleEnd));
                }
            }
            
            int nActual = nDesiredNewSynapses < eligibleEnd ? nDesiredNewSynapses : eligibleEnd;
            
            for(int i = 0;i < nActual;i++) {
                int rand = random.nextInt(eligibleEnd);
                segment.createSynapse(conn, candidates.get(rand), initialPermanence);
                eligibleEnd--;
                candidates.set(rand, candidates.get(eligibleEnd));
            }
        }
    
    /**
     * Container for {@link OldTemporalMemory#excitedColumnsGenerator(int[], List, List, Connections)} cycle
     */
    public static class ExcitedColumn implements Serializable {
        /** serial version */
        private static final long serialVersionUID = 1L;
        
        int column;
        int activeSegmentsCount;
        int matchingSegmentsCount;
        
        boolean isActiveColumn;
        
        Generator<DistalDendrite> activeSegments;
        Generator<DistalDendrite> matchingSegments;
        

        public ExcitedColumn(int column, boolean isActiveColumn, Generator<DistalDendrite> activeSegments,
            int activeSegmentsCount, Generator<DistalDendrite> matchingSegments, int matchingSegmentsCount) {
            
            this.column = column;
            this.isActiveColumn = isActiveColumn;
            this.activeSegments = activeSegments;
            this.activeSegmentsCount = activeSegmentsCount;
            this.matchingSegments = matchingSegments;
            this.matchingSegmentsCount = matchingSegmentsCount;
        }
    }
}
