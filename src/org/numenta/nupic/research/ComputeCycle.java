package org.numenta.nupic.research;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;

/**
 * Container for the many {@link Set}s maintained by the {@link TemporalMemory}
 * for computation results, and interim computation results.
 * 
 * @author David Ray
 */
public class ComputeCycle {
	Set<Cell> activeCells = new LinkedHashSet<Cell>();
	Set<Cell> winnerCells = new LinkedHashSet<Cell>();
	Set<Cell> predictiveCells = new LinkedHashSet<Cell>();
	Set<Column> predictedColumns = new LinkedHashSet<Column>();
	Set<Segment> activeSegments = new LinkedHashSet<Segment>();
	Set<Segment> learningSegments = new LinkedHashSet<Segment>();
	Map<Segment, Set<Synapse>> activeSynapsesForSegment = new LinkedHashMap<Segment, Set<Synapse>>();
	
	
	/**
	 * Constructs a new {@code ComputeCycle}
	 */
	public ComputeCycle() {}
	
	/**
	 * Constructs a new {@code ComputeCycle} initialized with
	 * the connections relevant to the current calling {@link Thread} for
	 * the specified {@link TemporalMemory}
	 * 
	 * @param	tm		the current TemporalMemory
	 */
	public ComputeCycle(TemporalMemory tm) {
		this.activeCells = new LinkedHashSet<Cell>(tm.getActiveCells());
		this.winnerCells = new LinkedHashSet<Cell>(tm.getWinnerCells());
		this.predictiveCells = new LinkedHashSet<Cell>(tm.getPredictiveCells());
		this.predictedColumns = new LinkedHashSet<Column>(tm.getPredictedColumns());
		this.activeSegments = new LinkedHashSet<Segment>(tm.getActiveSegments());
		this.learningSegments = new LinkedHashSet<Segment>(tm.getLearningSegments());
		this.activeSynapsesForSegment = new LinkedHashMap<Segment, Set<Synapse>>(tm.getActiveSynapsesForSegment());
	}
	
	/**
	 * Returns the current {@link Set} of active cells
	 * 
	 * @return	the current {@link Set} of active cells
	 */
	public Set<Cell> activeCells() {
		return activeCells;
	}
	
	/**
	 * Returns the current {@link Set} of winner cells
	 * 
	 * @return	the current {@link Set} of winner cells
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
	 * Returns the current {@link Set} of predicted columns
	 * 
	 * @return	the current {@link Set} of predicted columns
	 */
	public Set<Column> predictedColumns() {
		return predictedColumns;
	}
	
	/**
	 * Returns the Set of learning {@link Segment}s
	 * @return
	 */
	public Set<Segment> learningSegments() {
		return learningSegments;
	}
	
	/**
	 * Returns the Set of active {@link Segment}s
	 * @return
	 */
	public Set<Segment> activeSegments() {
		return activeSegments;
	}
	
	/**
	 * Returns the mapping of Segments to active synapses in t-1
	 * @return
	 */
	public Map<Segment, Set<Synapse>> activeSynapsesForSegment() {
		return activeSynapsesForSegment;
	}
}
