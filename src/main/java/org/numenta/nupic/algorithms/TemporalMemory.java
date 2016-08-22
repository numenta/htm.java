package org.numenta.nupic.algorithms;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.GroupBy2;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;

import javafx.util.Pair;

public class TemporalMemory implements ComputeDecorator, Serializable {
    private static final long serialVersionUID = 1L;
    
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
        Function<DistalDendrite, Column> segToCol = segment -> segment.getParentCell().getColumn(); 
        
        GroupBy2<Column> grouper = GroupBy2.<Column>of(
            new Pair(activeColumns, identity),
            new Pair(new ArrayList(conn.getActiveSegments()), segToCol),
            new Pair(new ArrayList(conn.getMatchingSegments()), segToCol));
        
        for(Tuple t : grouper) {
            ColumnData columnData = new ColumnData(t);
            System.out.println("Column: " + columnData.column());
            System.out.println("activeColumns: " + columnData.activeColumns());
            System.out.println("activeSegmentsOnCol: " + columnData.activeSegments());
            System.out.println("matchingSegmentsOnCol: " + columnData.matchingSegments());
            
            if(!columnData.activeColumns().isEmpty()) {
                if(!columnData.activeSegments().isEmpty()) {
                    
                }
            }
        }
            
        
        return null;
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
     * @param activeSegments            A iterable of Segment objects for the
     *                                  column compute is operating on that are active
     * @param prevActiveCells           Active cells in `t-1`
     * @param permanenceIncrement       Amount by which permanences of synapses are
     *                                  incremented during learning.
     * @param permanenceDecrement       Amount by which permanences of synapses are
     *                                  decremented during learning.
     * @param learn
     * @return      A list of predicted cells that will be added to active cells and winner cells.
     */
    public List<Cell> activatePredictedColumn(Connections conn, List<DistalDendrite> activeSegments,
        Set<Cell> prevActiveCells, double permanenceIncrement, double permanenceDecrement, boolean learn) {
        
        List<Cell> cellsToAdd = new ArrayList<>();
        Cell cell = null;
        for(DistalDendrite active : activeSegments) {
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
     * @param column                    The {@link Column} which is bursting 
     * @param matchingSegments          A list of {@link DistalDendrites} for the column compute
     *                                  is operating on that are matching                       
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
//    public Tuple burstColumn(Connections conn, Column column, List<DistalDendrite> matchingSegments, Set<Cell> prevActiveCells, Set<Cell> prevWinnerCells,
//        int maxNewSynapseCount, double initialPermanence, double permanenceIncrement, double permanenceDecrement, Random random, boolean learn) {
//        
//        List<Cell> cells = column.getCells();
//        Cell bestCell = null;
//        
//        if(!matchingSegments.isEmpty()) {
//            DistalDendrite bestSegment = matchingSegments.stream().max(s -> s.getOverlap());
//            
//            DistalDendrite bestSegment = (DistalDendrite)bestMatch.get(0);
//            bestCell = bestSegment.getParentCell();
//            
//            if(learn) {
//                bestSegment.adaptSegment(prevActiveCells, conn, permanenceIncrement, permanenceDecrement);
//                
//                int nGrowDesired = maxNewSynapseCount - (int)bestMatch.get(1);
//                if(nGrowDesired > 0) {
//                    growSynapses(conn, prevWinnerCells, bestSegment, initialPermanence, nGrowDesired, random);
//                }
//            }
//        } else {
//            bestCell = leastUsedCell(conn, cells, random, learn);
//            if(learn) {
//                int nGrowExact = Integer.min(maxNewSynapseCount, prevWinnerCells.size());
//                if(nGrowExact > 0) {
//                    DistalDendrite bestSegment = bestCell.createSegment(conn);
//                    growSynapses(conn, prevWinnerCells, bestSegment, initialPermanence, nGrowExact, random);
//                }
//            }
//        }
//        
//        return new Tuple(cells, bestCell);
//    }

    @Override
    public void reset(Connections connections) {
        // TODO Auto-generated method stub
        
    }
    
    @SuppressWarnings("unchecked")
    class ColumnData {
        Tuple t;
        public ColumnData(Tuple t) {
            this.t = t;
        }
        
        public Column column() { return (Column)t.get(0); }
        public List<Column> activeColumns() { return (List<Column>)t.get(1); }
        public List<DistalDendrite> activeSegments() { 
            return t.get(2).equals(Optional.empty()) ? 
                Collections.emptyList() :
                    (List<DistalDendrite>)t.get(2); 
        }
        public List<DistalDendrite> matchingSegments() {
            return t.get(3).equals(Optional.empty()) ? 
                Collections.emptyList() :
                    (List<DistalDendrite>)t.get(3); 
        }
    }

}
