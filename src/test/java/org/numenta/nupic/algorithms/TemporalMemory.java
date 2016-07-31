package org.numenta.nupic.algorithms;

import java.io.Serializable;
import java.util.List;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.AbstractGenerator;
import org.numenta.nupic.util.Generator;
import org.numenta.nupic.util.IntGenerator;
import org.numenta.nupic.util.NamedTuple;
import org.numenta.nupic.util.SegmentGenerator;
import org.numenta.nupic.util.SparseObjectMatrix;

public class TemporalMemory implements ComputeDecorator, Serializable {
	private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code TemporalMemory}
     */
	public TemporalMemory() {}
	
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
    private Generator<NamedTuple> excitedColumnsGenerator(
        List<Column> activeColumns, List<DistalDendrite> activeSegments, List<DistalDendrite> matchingSegments,
            Connections connections) {
        
        return new AbstractGenerator<NamedTuple>() {
            int activeColumnsProcessed = 0;
            int activeSegmentsProcessed = 0;
            int matchingSegmentsProcessed = 0;

            int activeColumnsNum = activeColumns.size();
            int activeSegmentsNum = activeSegments.size();
            int matchingSegmentsNum = matchingSegments.size();
            
            boolean isActiveColumn;
            
            @Override
            public void exec() {
                while((activeColumnsProcessed < activeColumnsNum ||
                      activeSegmentsProcessed < activeSegmentsNum ||
                      matchingSegmentsProcessed < matchingSegmentsNum) && !haltRequested()) {
                    
                    int currentColumn = Integer.MAX_VALUE;
                    if(activeSegmentsProcessed < activeSegmentsNum) {
                        currentColumn = Math.min(
                            currentColumn, 
                            activeSegments.get(activeSegmentsProcessed).getPresynapticColumnIndex());
                    }
                    
                    if(matchingSegmentsProcessed < matchingSegmentsNum) {
                        currentColumn = Math.min(
                            currentColumn,
                            matchingSegments.get(matchingSegmentsProcessed).getPresynapticColumnIndex());
                    }
                    
                    if(activeColumnsProcessed < activeColumnsNum &&
                        activeColumns.get(activeColumnsProcessed).getIndex() <= currentColumn) {
                        
                        currentColumn = activeColumns.get(activeColumnsProcessed).getIndex();
                        isActiveColumn = true;
                        activeColumnsProcessed += 1;
                    } else {
                        isActiveColumn = false;
                    }
                    
                    int activeSegmentsBegin = activeSegmentsProcessed;
                    int activeSegmentsEnd = activeSegmentsProcessed;
                    for(int i = activeSegmentsProcessed;i < activeSegmentsNum;i++) {
                        if(activeSegments.get(i).getPresynapticColumnIndex() == currentColumn) {
                            activeSegmentsProcessed += 1;
                            activeSegmentsEnd += 1;
                        } else {
                            break;
                        }
                    }
                    
                    int matchingSegmentsBegin = matchingSegmentsProcessed;
                    int matchingSegmentsEnd = matchingSegmentsProcessed;
                    for(int i = matchingSegmentsProcessed;i < matchingSegmentsNum;i++) {
                        if(matchingSegments.get(i).getPresynapticColumnIndex() == currentColumn) {
                            matchingSegmentsProcessed += 1;
                            matchingSegmentsEnd += 1;
                        } else {
                            break;
                        }
                    }
                    
                    Generator<Integer> asIndexGenerator = IntGenerator.of(activeSegmentsBegin, activeSegmentsEnd);
                    Generator<Integer> msIndexGenerator = IntGenerator.of(matchingSegmentsBegin, matchingSegmentsEnd);
                    yield(
                        new NamedTuple(
                            new String[] { 
                                "column", 
                                "isActiveColumn",
                                "activeSegments", 
                                "activeSegmentsCount", 
                                "matchingSegments", 
                                "matchingSegmentsCount" 
                            },
                            currentColumn, 
                            isActiveColumn, 
                            SegmentGenerator.of(activeSegments, asIndexGenerator), 
                            activeSegmentsEnd - activeSegmentsBegin,
                            SegmentGenerator.of(matchingSegments, msIndexGenerator),
                            matchingSegmentsEnd - matchingSegmentsBegin
                        )
                    );
                }
            }

            @Override
            public boolean isConsumed() {
                return activeColumnsProcessed >= activeColumnsNum &&
                       activeSegmentsProcessed >= activeSegmentsNum &&
                       matchingSegmentsProcessed >= matchingSegmentsNum;
            }
        };
    }

	/////////////////////////// CORE FUNCTIONS /////////////////////////////
    
    /**
     * Feeds input record through TM, performing inference and learning.
     * 
     * <pre>
     * Pseudocode:
     *	for each column
     *      if column is active and has active distal dendrite segments
     *          call activatePredictedColumn
     *        if column is active and doesn't have active distal dendrite segments
     *          call burstColumn
     *        if column is inactive and has matching distal dendrite segments
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
     *
     * @param activeColumns (set)  Indices of active columns
     * @param learn         (boolean) Whether or not learning is enabled
     */
    @Override
    public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void reset(Connections connections) {
        // TODO Auto-generated method stub

    }
}
