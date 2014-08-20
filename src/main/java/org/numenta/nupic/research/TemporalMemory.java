package org.numenta.nupic.research;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;

/**
 * Temporal Memory implementation in Java
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class TemporalMemory {
	/** Total number of columns */
	protected int[] columnDimensions = new int[] { 2048 };
	/** Total number of cells per column */
	protected int cellsPerColumn = 32;
	/** 
	 * If the number of active connected synapses on a segment 
	 * is at least this threshold, the segment is said to be active.
     */
	private int activationThreshold = 13;
	/**
	 * Radius around cell from which it can
     * sample to form distal {@link Segment} connections.
	 */
	@SuppressWarnings("unused")
	private int learningRadius = 2048;
	/**
	 * If the number of synapses active on a segment is at least this
	 * threshold, it is selected as the best matching
     * cell in a bursting column.
	 */
	private int minThreshold = 10;
	/** The maximum number of synapses added to a segment during learning. */
	private int maxNewSynapseCount = 20;
	/** Initial permanence of a new synapse */
	private double initialPermanence = 0.21;
	/**
	 * If the permanence value for a synapse
	 * is greater than this value, it is said
     * to be connected.
	 */
	private double connectedPermanence = 0.50;
	/** 
	 * Amount by which permanences of synapses
	 * are incremented during learning.
	 */
	private double permanenceIncrement = 0.10;
	/** 
	 * Amount by which permanences of synapses
	 * are decremented during learning.
	 */
	private double permanenceDecrement = 0.10;
	/**
	 * The connection data state for this {@code TemporalMemory}
	 */
	private Connections connections = new Connections();
	
	
	private Cell[] cells;
	
	private List<Column> columns = new ArrayList<Column>(columnDimensions[0]);
	
	
	/**
	 * Constructs a new {@code TemporalMemory}
	 */
	public TemporalMemory() {
		this(null);
	}
	
	/**
	 * Constructs a new {@code TemporalMemory} from a configured
	 * {@link Parameters} object.
	 * @param params
	 */
	public TemporalMemory(Parameters params) {
		if(params != null) {
			Parameters.apply(this, params);
		}
	}
	
	/**
	 * Builds the grid of {@link Cell}s and  {@link Column}s
	 */
	public void init() {
		cells = new Cell[columnDimensions[0] * cellsPerColumn];
		
		for(int i = 0;i < columnDimensions[0];i++) {
			Column column = new Column(cellsPerColumn, i);
			columns.add(column);
			for(int j = 0;j < cellsPerColumn;j++) {
				cells[i * cellsPerColumn + j] = column.getCell(j);
			}
		}
	}
	
	/**
	 * Returns the seeded random number generator.
	 * @return
	 */
	public Random getRandom() {
		return connections.random();
	}
	
	/**
	 * Returns the column at the specified index.
	 * @param index
	 * @return
	 */
	public Column getColumn(int index) {
		return columns.get(index);
	}
	
	/**
	 * Sets the number of {@link Column}.
	 * 
	 * @param columnDimensions
	 */
	public void setColumnDimensions(int[] columnDimensions) {
		this.columnDimensions = columnDimensions;
	}
	
	/**
	 * Gets the number of {@link Column}.
	 * 
	 * @return columnDimensions
	 */
	public int[] getColumnDimensions() {
		return this.columnDimensions;
	}

	/**
	 * Sets the number of {@link Cell}s per {@link Column}
	 * @param cellsPerColumn
	 */
	public void setCellsPerColumn(int cellsPerColumn) {
		this.cellsPerColumn = cellsPerColumn;
	}
	
	/**
	 * Gets the number of {@link Cells} per {@link Column}.
	 * 
	 * @return cellsPerColumn
	 */
	public int getCellsPerColumn() {
		return this.cellsPerColumn;
	}

	/**
	 * Sets the activation threshold.
	 * 
	 * If the number of active connected synapses on a segment 
	 * is at least this threshold, the segment is said to be active.
	 * 
	 * @param activationThreshold
	 */
	public void setActivationThreshold(int activationThreshold) {
		this.activationThreshold = activationThreshold;
	}

	/**
	 * Radius around cell from which it can
     * sample to form distal dendrite connections.
     * 
     * @param	learningRadius
	 */
	public void setLearningRadius(int learningRadius) {
		this.learningRadius = learningRadius;
	}

	/**
	 * If the number of synapses active on a segment is at least this
	 * threshold, it is selected as the best matching
     * cell in a bursing column.
     * 
     * @param	minThreshold
	 */
	public void setMinThreshold(int minThreshold) {
		this.minThreshold = minThreshold;
	}

	/** 
	 * The maximum number of synapses added to a segment during learning. 
	 * 
	 * @param	maxNewSynapseCount
	 */
	public void setMaxNewSynapseCount(int maxNewSynapseCount) {
		this.maxNewSynapseCount = maxNewSynapseCount;
	}

	/** 
	 * Seed for random number generator 
	 * 
	 * @param	seed
	 */
	public void setSeed(int seed) {
		connections.setSeed(seed);
	}
	
	/** 
	 * Initial permanence of a new synapse 
	 * 
	 * @param	
	 */
	public void setInitialPermanence(double initialPermanence) {
		this.initialPermanence = initialPermanence;
	}
	
	/**
	 * If the permanence value for a synapse
	 * is greater than this value, it is said
     * to be connected.
     * 
     * @param connectedPermanence
	 */
	public void setConnectedPermanence(double connectedPermanence) {
		this.connectedPermanence = connectedPermanence;
	}

	/** 
	 * Amount by which permanences of synapses
	 * are incremented during learning.
	 * 
	 * @param 	permanenceIncrement
	 */
	public void setPermanenceIncrement(double permanenceIncrement) {
		this.permanenceIncrement = permanenceIncrement;
	}
	
	/** 
	 * Amount by which permanences of synapses
	 * are incremented during learning.
	 * 
	 * @param 	permanenceIncrement
	 */
	public double getPermanenceIncrement() {
		return this.permanenceIncrement;
	}

	/** 
	 * Amount by which permanences of synapses
	 * are decremented during learning.
	 * 
	 * @param	permanenceDecrement
	 */
	public void setPermanenceDecrement(double permanenceDecrement) {
		this.permanenceDecrement = permanenceDecrement;
	}
	
	/** 
	 * Amount by which permanences of synapses
	 * are decremented during learning.
	 * 
	 * @param	permanenceDecrement
	 */
	public double getPermanenceDecrement() {
		return this.permanenceDecrement;
	}
	
	/////////////////////////// CORE FUNCTIONS /////////////////////////////
	
	/**
	 * Feeds input record through TM, performing inferencing and learning
	 * 
	 * @param activeColumns		direct proximal dendrite input
	 * @param learn				learning mode flag
	 * @return					{@link ComputeCycle} container for one cycle of inference values.
	 */
	public ComputeCycle compute(int[] activeColumns, boolean learn) {
		ComputeCycle result = computeFn(connections, getColumns(activeColumns), new LinkedHashSet<Cell>(connections.predictiveCells), 
			new LinkedHashSet<Segment>(connections.activeSegments), new LinkedHashMap<Segment, Set<Synapse>>(connections.activeSynapsesForSegment), 
				new LinkedHashSet<Cell>(connections.winnerCells), learn);
		
		connections.activeCells = result.activeCells();
		connections.winnerCells = result.winnerCells();
		connections.predictiveCells = result.predictiveCells();
		connections.predictedColumns = result.predictedColumns();
		connections.activeSegments = result.activeSegments();
		connections.learningSegments = result.learningSegments();
		connections.activeSynapsesForSegment = result.activeSynapsesForSegment();
		
		return result; 
	}
	
	/**
	 * Functional version of {@link #compute(int[], boolean)}. 
	 * This method is stateless and concurrency safe.
	 * 
	 * @param c								{@link Connections} object containing state of memory members
	 * @param activeColumns					proximal dendrite input
	 * @param prevPredictiveCells			cells predicting in t-1
	 * @param prevActiveSegments			active segments in t-1
	 * @param prevActiveSynapsesForSegment	{@link Synapse}s active in t-1
	 * @param prevWinnerCells	`			previous winners
	 * @param learn							whether mode is "learning" mode
	 * @return
	 */
	public ComputeCycle computeFn(Connections c, Set<Column> activeColumns, Set<Cell> prevPredictiveCells, Set<Segment> prevActiveSegments,
		Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment, Set<Cell> prevWinnerCells, boolean learn) {
		
		ComputeCycle cycle = new ComputeCycle();
		
		activateCorrectlyPredictiveCells(cycle, prevPredictiveCells, activeColumns);
		
		burstColumns(cycle, c, activeColumns, cycle.predictedColumns, prevActiveSynapsesForSegment);
		
		if(learn) {
			learnOnSegments(c, prevActiveSegments, cycle.learningSegments, prevActiveSynapsesForSegment, cycle.winnerCells, prevWinnerCells);
		}
		
		cycle.activeSynapsesForSegment = computeActiveSynapses(c, cycle.activeCells);
		
		computePredictiveCells(cycle, cycle.activeSynapsesForSegment);
		
		return cycle;
	}

	/**
	 * Phase 1: Activate the correctly predictive cells
	 * 
	 * Pseudocode:
     *
     * - for each prev predictive cell
     *   - if in active column
     *     - mark it as active
     *     - mark it as winner cell
     *     - mark column as predicted
     *     
	 * @param c						ComputeCycle interim values container
	 * @param prevPredictiveCells	predictive {@link Cell}s predictive cells in t-1
	 * @param activeColumns			active columns in t
	 */
	public void activateCorrectlyPredictiveCells(ComputeCycle c, Set<Cell> prevPredictiveCells, Set<Column> activeColumns) {
		for(Cell cell : prevPredictiveCells) {
			Column column = cell.getParentColumn();
			if(activeColumns.contains(column)) {
				c.activeCells.add(cell);
				c.winnerCells.add(cell);
				c.predictedColumns.add(column);
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
     *         - (optimization) if there are prev winner cells
     *           - add a segment to it
     *       - mark the segment as learning
	 * 
	 * @param cycle						    ComputeCycle interim values container
	 * @param c								Connections temporal memory state
	 * @param activeColumns					active columns in t
	 * @param predictedColumns				predicted columns in t
	 * @param prevActiveSynapsesForSegment		LinkedHashMap of previously active segments which
	 * 										have had synapses marked as active in t-1     
	 */
	public void burstColumns(ComputeCycle cycle, Connections c, Set<Column> activeColumns, Set<Column> predictedColumns, 
		Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment) {
		
		Set<Column> unpred = new LinkedHashSet<Column>(activeColumns);
		
		unpred.removeAll(predictedColumns);
		for(Column column : unpred) {
			List<Cell> cells = column.getCells();
			cycle.activeCells.addAll(cells);
			
			Object[] bestSegmentAndCell = getBestMatchingCell(c, column, prevActiveSynapsesForSegment);
			Segment bestSegment = (Segment)bestSegmentAndCell[0];
			Cell bestCell = (Cell)bestSegmentAndCell[1];
			if(bestCell != null) {
				cycle.winnerCells.add(bestCell);
			}
			
			if(bestSegment == null) {
				bestSegment = bestCell.createSegment(c, c.segmentCounter);
				c.segmentCounter += 1;
			}
			
			cycle.learningSegments.add(bestSegment);
		}
	}
	
	/**
	 * Phase 3: Perform learning by adapting segments.
	 * <pre>
	 * Pseudocode:
     *
     * - (learning) for each prev active or learning segment
     *   - if learning segment or from winner cell
     *     - strengthen active synapses
     *     - weaken inactive synapses
     *   - if learning segment
     *     - add some synapses to the segment
     *       - subsample from prev winner cells
     * </pre>     
     *       
     * @param c								the Connections state of the temporal memory
	 * @param prevActiveSegments
	 * @param learningSegments
	 * @param prevActiveSynapseSegments
	 * @param winnerCells
	 * @param prevWinnerCells
	 */
	public void learnOnSegments(Connections c, Set<Segment> prevActiveSegments, Set<Segment> learningSegments,
		Map<Segment, Set<Synapse>> prevActiveSynapseSegments, Set<Cell> winnerCells, Set<Cell> prevWinnerCells) {
		
		List<Segment> prevAndLearning = new ArrayList<Segment>(prevActiveSegments);
		prevAndLearning.addAll(learningSegments);
		
		for(Segment dd : prevAndLearning) {
			boolean isLearningSegment = learningSegments.contains(dd);
			boolean isFromWinnerCell = winnerCells.contains(dd.getParentCell());
			
			Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>(dd.getConnectedActiveSynapses(prevActiveSynapseSegments, 0));
			
			if(isLearningSegment || isFromWinnerCell) {
				dd.adaptSegment(c, activeSynapses, permanenceIncrement, permanenceDecrement);
			}
			
			if(isLearningSegment) {
				int n = maxNewSynapseCount - activeSynapses.size();
				Set<Cell> learnCells = dd.pickCellsToLearnOn(c, n, prevWinnerCells, c.random);
				for(Cell sourceCell : learnCells) {
					dd.createSynapse(c, sourceCell, initialPermanence, c.synapseCounter);
					c.synapseCounter += 1;
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
	 * @param c					the Connections state of the temporal memory
	 * @param activeSegments
	 */
	public void computePredictiveCells(ComputeCycle cycle, Map<Segment, Set<Synapse>> activeDendrites) {
		for(Segment dd : activeDendrites.keySet()) {
			Set<Synapse> connectedActive = dd.getConnectedActiveSynapses(activeDendrites, connectedPermanence);
			if(connectedActive.size() >= activationThreshold) {
				cycle.activeSegments.add(dd);
				cycle.predictiveCells.add(dd.getParentCell());
			}
		}
	}
	
	/**
	 * Forward propagates activity from active cells to the synapses that touch
     * them, to determine which synapses are active.
     * 
     * @param	c			the connections state of the temporal memory
	 * @param cellsActive
	 * @return 
	 */
	public Map<Segment, Set<Synapse>> computeActiveSynapses(Connections c, Set<Cell> cellsActive) {
		Map<Segment, Set<Synapse>> activesSynapses = new LinkedHashMap<Segment, Set<Synapse>>();
		
		for(Cell cell : cellsActive) {
			for(Synapse s : cell.getReceptorSynapses(c)) {
				Set<Synapse> set = null;
				if((set = activesSynapses.get(s.getSegment())) == null) {
					activesSynapses.put(s.getSegment(), set = new LinkedHashSet<Synapse>());
				}
				set.add(s);
			}
		}
		
		return activesSynapses;
	}
	
	/**
	 * Called to start the input of a new sequence.
	 * 
	 * @param	c 	the Connections state of the temporal memory
	 */
	public void reset() {
		connections.activeCells.clear();
		connections.predictiveCells.clear();
		connections.activeSegments.clear();
		connections.activeSynapsesForSegment.clear();
		connections.winnerCells.clear();
	}
	
	//////////////////////// RESULT FUNCTIONS ///////////////////////
	public Connections getConnections() {
		return this.connections;
	}
	/**
	 * Returns the current {@link Set} of active cells
	 * 
	 * @return	the current {@link Set} of active cells
	 */
	public Set<Cell> getActiveCells() {
		return connections.activeCells();
	}
	
	/**
	 * Returns the current {@link Set} of winner cells
	 * 
	 * @return	the current {@link Set} of winner cells
	 */
	public Set<Cell> getWinnerCells() {
		return connections.winnerCells();
	}
	
	/**
	 * Returns the {@link Set} of predictive cells.
	 * @return
	 */
	public Set<Cell> getPredictiveCells() {
		return connections.predictiveCells();
	}
	
	/**
	 * Returns the current {@link Set} of predicted columns
	 * 
	 * @return	the current {@link Set} of predicted columns
	 */
	public Set<Column> getPredictedColumns() {
		return connections.predictedColumns();
	}
	
	/**
	 * Returns the Set of learning {@link Segment}s
	 * @return
	 */
	public Set<Segment> getLearningSegments() {
		return connections.learningSegments();
	}
	
	/**
	 * Returns the Set of active {@link Segment}s
	 * @return
	 */
	public Set<Segment> getActiveSegments() {
		return connections.activeSegments();
	}
	
	/**
	 * Returns a mapping of synapses that have become active to their
	 * segments.
	 * 
	 * @return
	 */
	public Map<Segment, Set<Synapse>> getActiveSynapsesForSegment() {
		return connections.activeSynapsesForSegment();
	}
	
	/////////////////////////// HELPER FUNCTIONS ///////////////////////////
	
	public List<Integer> asCellIndexes(Collection<Cell> cells) {
		List<Integer> ints = new ArrayList<Integer>();
		for(Cell cell : cells) {
			ints.add(cell.getIndex());
		}
		
		return ints;
	}
	
	public List<Integer> asColumnIndexes(Collection<Column> columns) {
		List<Integer> ints = new ArrayList<Integer>();
		for(Column col : columns) {
			ints.add(col.getIndex());
		}
		
		return ints;
	}
	
	public List<Cell> asCellObjects(Collection<Integer> cells) {
		List<Cell> objs = new ArrayList<Cell>();
		for(int i : cells) {
			objs.add(this.cells[i]);
		}
		return objs;
	}
	
	public List<Column> asColumnObjects(Collection<Integer> cols) {
		List<Column> objs = new ArrayList<Column>();
		for(int i : cols) {
			objs.add(this.columns.get(i));
		}
		return objs;
	}
	
	public Object[] getBestMatchingCell(Connections c, Column column, Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment) {
		Object[] retVal = new Object[2];
		Cell bestCell = null;
		Segment bestSegment = null;
		int maxSynapses = 0;
		for(Cell cell : column.getCells()) {
			Segment dd = getBestMatchingSegment(c, cell, prevActiveSynapsesForSegment);
			if(dd != null) {
				Set<Synapse> connectedActiveSynapses = dd.getConnectedActiveSynapses(prevActiveSynapsesForSegment, 0);
				if(connectedActiveSynapses.size() > maxSynapses) {
					maxSynapses = connectedActiveSynapses.size();
					bestCell = cell;
					bestSegment = dd;
				}
			}
		}
		
		if(bestCell == null) {
			bestCell = column.getLeastUsedCell(c, c.random());
		}
		
		retVal[0] = bestSegment;
		retVal[1] = bestCell;
		return retVal;
	}
	
	public Segment getBestMatchingSegment(Connections c, Cell cell, Map<Segment, Set<Synapse>> activeSynapseSegments) {
		int maxSynapses = minThreshold;
		Segment bestSegment = null;
		for(Segment dd : cell.getSegments(c)) {
			Set<Synapse> activeSyns = dd.getConnectedActiveSynapses(activeSynapseSegments, 0);
			if(activeSyns.size() >= maxSynapses) {
				maxSynapses = activeSyns.size();
				bestSegment = dd;
			}
		}
		return bestSegment;
	}
	
	/**
	 * Returns the column index given the cells per column and
	 * the cell index passed in.
	 * 
	 * @param cellIndex
	 * @return
	 */
	protected int columnForCell(int cellIndex) {
		return cellIndex / cellsPerColumn;
	}
	
	/**
	 * Returns the cell at the specified index.
	 * @param index
	 * @return
	 */
	public Cell getCell(int index) {
		return cells[index];
	}
	
	/**
	 * Returns a {@link LinkedHashSet} of {@link Cell}s from a 
	 * sorted array of cell indexes.
	 *  
	 * @param cellIndexes	indexes of the {@link Cell}s to return
	 * @return
	 */
	public LinkedHashSet<Cell> getCells(int[] cellIndexes) {
		LinkedHashSet<Cell> cellSet = new LinkedHashSet<Cell>();
		for(int cell : cellIndexes) {
			cellSet.add(cells[cell]);
		}
		return cellSet;
	}
	
	/**
	 * Returns a {@link LinkedHashSet} of {@link Column}s from a 
	 * sorted array of Column indexes.
	 *  
	 * @param cellIndexes	indexes of the {@link Column}s to return
	 * @return
	 */
	public LinkedHashSet<Column> getColumns(int[] columnIndexes) {
		LinkedHashSet<Column> columnSet = new LinkedHashSet<Column>();
		for(int column : columnIndexes) {
			columnSet.add(columns.get(column));
		}
		return columnSet;
	}
	
	/**
	 * Subtracts the contents of the first argument from the last argument's list.
	 * 
	 * <em>NOTE: Does not destroy/alter the argument lists. </em>
	 * 
	 * @param minuend
	 * @param subtrahend
	 * @return
	 */
	public List<Integer> subtract(List<Integer> subtrahend, List<Integer> minuend) {
		ArrayList<Integer> sList = new ArrayList<Integer>(minuend);
		sList.removeAll(subtrahend);
		return new ArrayList<Integer>(sList);
	}
	
}
