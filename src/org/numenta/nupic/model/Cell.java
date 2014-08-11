package org.numenta.nupic.model;

import java.util.List;
import java.util.Set;

import org.numenta.nupic.research.Connections;

/**
 * Software implementation of a neuron in the neocortical region.
 * 
 * @author David Ray
 */
public class Cell implements Comparable<Cell> {
	/** This cell's index */
	private final int index;
	/** The owning {@link Column} */
	private final Column parentColumn;
	
	
	/**
	 * Constructs a new {@code Cell} object
	 * @param column	the parent {@link Column}
	 * @param index		this {@code Cell}'s index
	 */
	public Cell(Column column, int index) {
		this.parentColumn = column;
		this.index = column.getIndex() * column.getNumCellsPerColumn() + index;
	}
	
	/**
	 * Returns this {@code Cell}'s index.
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Returns the column within which this cell resides
	 * @return
	 */
	public Column getParentColumn() {
		return parentColumn;
	}
	
	/**
	 * Adds a {@link Synapse} which is the receiver of signals
	 * from this {@code Cell}
	 * 
	 * @param c		the connections state of the temporal memory
	 * @param s
	 */
	public void addReceptorSynapse(Connections c, Synapse s) {
		c.receptorSynapses(this).add(s);
	}
	
	/**
	 * Returns the Set of {@link Synapse}s which have this cell
	 * as their source cells.
	 * 	
	 * @param 	c		the connections state of the temporal memory
	 * @return	the Set of {@link Synapse}s which have this cell
	 * 			as their source cells.
	 */
	public Set<Synapse> getReceptorSynapses(Connections c) {
		return c.receptorSynapses(this);
	}
	
	/**
	 * Returns a newly created {@link Segment}
	 * 
	 * @param	c		the connections state of the temporal memory
	 * @param index		the index of the new {@link Segment}
	 * @return			 a newly created {@link Segment}
	 */
	public Segment createSegment(Connections c, int index) {
		Segment dd = new Segment(this, index);
		c.segments(this).add(dd);
		
		return dd;
	}
	
	/**
	 * Returns a {@link List} of this {@code Cell}'s {@link Segment}s
	 * 
	 * @param	c	the connections state of the temporal memory
	 * @return	a {@link List} of this {@code Cell}'s {@link Segment}s
	 */
	public List<Segment> getSegments(Connections c) {
		return c.segments(this);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "Cell: col=" + parentColumn.getIndex() + ", idx=" + index;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <em> Note: All comparisons use the cell's index only </em>
	 */
	@Override
	public int compareTo(Cell arg0) {
		return new Integer(index).compareTo(arg0.getIndex());
	}
}
