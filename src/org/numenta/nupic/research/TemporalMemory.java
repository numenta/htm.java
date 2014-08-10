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
 * @author David Ray
 */
public class TemporalMemory {
	/** Total number of columns */
	protected int columnDimensions = 2048;
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
	
	
	
	private Cell[] cells;
	
	private List<Column> columns = new ArrayList<Column>(columnDimensions);
	
	/** Contains thread specific connection mapping	 */
	private static ThreadLocal<Connections> threadLocal = new ThreadLocal<Connections>();
	
	
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
		cells = new Cell[columnDimensions * cellsPerColumn];
		
		for(int i = 0;i < columnDimensions;i++) {
			Column column = new Column(cellsPerColumn, i);
			columns.add(column);
			for(int j = 0;j < cellsPerColumn;j++) {
				cells[i * cellsPerColumn + j] = column.getCell(j);
			}
		}
	}
	
	/**
	 * Returns a thread specific {@link Connections} object.
	 * @return
	 */
	public static Connections get() {
		Connections retVal = null;
		if((retVal = threadLocal.get()) == null) {
			threadLocal.set(retVal = new Connections());
		}
		return retVal;
	}
	
	/**
	 * Sets the {@link Connections} object to be used by the
	 * current calling {@link Thread}. If a pre-existing {@code Connections}
	 * object is mapped, that object is first cleared of all of its resources
	 * prior to the specified {@code Connections} object being set.
	 * 
	 * @param c
	 */
	public static void set(Connections c) {
		if(threadLocal.get() != null) {
			threadLocal.get().clear();
		}
		threadLocal.set(c);
	}
	
	/**
	 * Removes the {@link Connections} object (if any exists) which is 
	 * mapped to the current calling {@link Thread}. If a pre-existing {@code Connections}
	 * object is mapped, that object is first cleared of all of its resources
	 * prior to its removal.
	 */
	public static void remove() {
		if(threadLocal.get() != null) {
			threadLocal.get().clear();
			threadLocal.remove();
		}
	}
	
	/**
	 * Returns the seeded random number generator.
	 * @return
	 */
	public Random getRandom() {
		return get().random();
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
	public void setColumnDimensions(int columnDimensions) {
		this.columnDimensions = columnDimensions;
	}
	
	/**
	 * Gets the number of {@link Column}.
	 * 
	 * @return columnDimensions
	 */
	public int getColumnDimensions() {
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
		get().setSeed(seed);
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
	
	public ComputeCycle compute(int[] activeColumns, boolean learn) {
		ComputeCycle result = computeFn(new ComputeCycle(), getColumns(activeColumns), new LinkedHashSet<Cell>(get().predictiveCells), 
			new LinkedHashSet<Segment>(get().activeSegments), new LinkedHashMap<Segment, Set<Synapse>>(get().activeSynapsesForSegment), 
				new LinkedHashSet<Cell>(get().winnerCells), learn);
		
		get().activeCells = result.activeCells();
		get().winnerCells = result.winnerCells();
		get().predictiveCells = result.predictiveCells();
		get().predictedColumns = result.predictedColumns();
		get().activeSegments = result.activeSegments();
		get().learningSegments = result.learningSegments();
		get().activeSynapsesForSegment = result.activeSynapsesForSegment();
		
		return result;  
	}
	
	public ComputeCycle computeFn(ComputeCycle computeCycle, Set<Column> activeColumns, Set<Cell> prevPredictiveCells, Set<Segment> prevActiveSegments,
		Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment, Set<Cell> prevWinnerCells, boolean learn) {
		
		activateCorrectlyPredictiveCells(computeCycle, prevPredictiveCells, activeColumns);
		
		burstColumns(computeCycle, activeColumns, computeCycle.predictedColumns, prevActiveSynapsesForSegment);
		
		if(learn) {
			learnOnSegments(prevActiveSegments, computeCycle.learningSegments, prevActiveSynapsesForSegment, computeCycle.winnerCells, prevWinnerCells);
		}
		
		computeCycle.activeSynapsesForSegment = computeActiveSynapses(computeCycle.activeCells);
		
		computePredictiveCells(computeCycle, computeCycle.activeSynapsesForSegment);
		
		return computeCycle;
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
	 * @param ComputeCycle				object to contain results.
	 * @param prevPredictiveCells	predictive {@link Cell}s predictive cells in t-1
	 * @param activeColumns			active columns in t
	 */
	public void activateCorrectlyPredictiveCells(ComputeCycle result, Set<Cell> prevPredictiveCells, Set<Column> activeColumns) {
		for(Cell cell : prevPredictiveCells) {
			Column column = cell.getParentColumn();
			if(activeColumns.contains(column)) {
				result.activeCells.add(cell);
				result.winnerCells.add(cell);
				result.predictedColumns.add(column);
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
	 * @param cycle							ComputeCycle object to contain result
	 * @param activeColumns					active columns in t
	 * @param predictedColumns				predicted columns in t
	 * @param prevActiveSynapsesForSegment		LinkedHashMap of previously active segments which
	 * 										have had synapses marked as active in t-1     
	 */
	public void burstColumns(ComputeCycle cycle, Set<Column> activeColumns, Set<Column> predictedColumns, 
		Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment) {
		
		Set<Column> unpred = new LinkedHashSet<Column>(activeColumns);
		
		unpred.removeAll(predictedColumns);
		for(Column column : unpred) {
			List<Cell> cells = column.getCells();
			cycle.activeCells.addAll(cells);
			
			Object[] bestSegmentAndCell = getBestMatchingCell(column, prevActiveSynapsesForSegment);
			Segment bestSegment = (Segment)bestSegmentAndCell[0];
			Cell bestCell = (Cell)bestSegmentAndCell[1];
			if(bestCell != null) {
				cycle.winnerCells.add(bestCell);
			}
			
			if(bestSegment == null) {
				bestSegment = bestCell.createSegment(get().segmentCounter);
				get().segmentCounter += 1;
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
	 * @param prevActiveSegments
	 * @param learningSegments
	 * @param prevActiveSynapseSegments
	 * @param winnerCells
	 * @param prevWinnerCells
	 */
	public void learnOnSegments(Set<Segment> prevActiveSegments, Set<Segment> learningSegments,
		Map<Segment, Set<Synapse>> prevActiveSynapseSegments, Set<Cell> winnerCells, Set<Cell> prevWinnerCells) {
		
		List<Segment> prevAndLearning = new ArrayList<Segment>(prevActiveSegments);
		prevAndLearning.addAll(learningSegments);
		
		for(Segment dd : prevAndLearning) {
			boolean isLearningSegment = learningSegments.contains(dd);
			boolean isFromWinnerCell = winnerCells.contains(dd.getParentCell());
			
			Set<Synapse> activeSynapses = new LinkedHashSet<Synapse>(dd.getConnectedActiveSynapses(prevActiveSynapseSegments, 0));
			
			if(isLearningSegment || isFromWinnerCell) {
				dd.adaptSegment(activeSynapses, permanenceIncrement, permanenceDecrement);
			}
			
			if(isLearningSegment) {
				int n = maxNewSynapseCount - activeSynapses.size();
				Set<Cell> learnCells = dd.pickCellsToLearnOn(n, prevWinnerCells, get().random);
				for(Cell sourceCell : learnCells) {
					dd.createSynapse(sourceCell, initialPermanence, get().synapseCounter);
					get().synapseCounter += 1;
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
	 * @param cycle			ComputeCycle object to contain results.
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
	 * @param cellsActive
	 * @return 
	 */
	public Map<Segment, Set<Synapse>> computeActiveSynapses(Set<Cell> cellsActive) {
		Map<Segment, Set<Synapse>> activesSynapses = new LinkedHashMap<Segment, Set<Synapse>>();
		
		for(Cell cell : cellsActive) {
			for(Synapse s : cell.getReceptorSynapses()) {
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
	 */
	public void reset() {
		get().activeCells.clear();
		get().predictiveCells.clear();
		get().activeSegments.clear();
		get().activeSynapsesForSegment.clear();
		get().winnerCells.clear();
	}
	
	//////////////////////// RESULT FUNCTIONS ///////////////////////
	
	/**
	 * Returns the current {@link Set} of active cells
	 * 
	 * @return	the current {@link Set} of active cells
	 */
	public Set<Cell> getActiveCells() {
		return get().activeCells();
	}
	
	/**
	 * Returns the current {@link Set} of winner cells
	 * 
	 * @return	the current {@link Set} of winner cells
	 */
	public Set<Cell> getWinnerCells() {
		return get().winnerCells();
	}
	
	/**
	 * Returns the {@link Set} of predictive cells.
	 * @return
	 */
	public Set<Cell> getPredictiveCells() {
		return get().predictiveCells();
	}
	
	/**
	 * Returns the current {@link Set} of predicted columns
	 * 
	 * @return	the current {@link Set} of predicted columns
	 */
	public Set<Column> getPredictedColumns() {
		return get().predictedColumns();
	}
	
	/**
	 * Returns the Set of learning {@link Segment}s
	 * @return
	 */
	public Set<Segment> getLearningSegments() {
		return get().learningSegments();
	}
	
	/**
	 * Returns the Set of active {@link Segment}s
	 * @return
	 */
	public Set<Segment> getActiveSegments() {
		return get().activeSegments();
	}
	
	/**
	 * Returns a mapping of synapses that have become active to their
	 * segments.
	 * 
	 * @return
	 */
	public Map<Segment, Set<Synapse>> getActiveSynapsesForSegment() {
		return get().activeSynapsesForSegment();
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
	
	public Object[] getBestMatchingCell(Column column, Map<Segment, Set<Synapse>> prevActiveSynapsesForSegment) {
		Object[] retVal = new Object[2];
		Cell bestCell = null;
		Segment bestSegment = null;
		int maxSynapses = 0;
		for(Cell cell : column.getCells()) {
			Segment dd = getBestMatchingSegment(cell, prevActiveSynapsesForSegment);
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
			bestCell = column.getLeastUsedCell(get().random());
		}
		
		retVal[0] = bestSegment;
		retVal[1] = bestCell;
		return retVal;
	}
	
	public Segment getBestMatchingSegment(Cell cell, Map<Segment, Set<Synapse>> activeSynapseSegments) {
		int maxSynapses = minThreshold;
		Segment bestSegment = null;
		for(Segment dd : cell.getSegments()) {
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
