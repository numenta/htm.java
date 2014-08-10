package org.numenta.nupic.model;

import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Represents a connection with varying strength which when above 
 * a configured threshold represents a valid connection. This class
 * may hold state because its scope is limited to a given {@link Thread}'s
 * {@link Connections} object.
 * 
 * @author David Ray
 * 
 * @see Segment
 * @see TemporalMemory.Connections
 */
public class Synapse {
	private Cell sourceCell;
	private Segment segment;
	private double permanence;
	private int index;
	
	/**
	 * Constructs a new {@code Synapse}
	 * 
	 * @param sourceCell	the {@link Cell} which will activate this {@code Synapse}
	 * @param segment		the owning dendritic segment
	 * @param permanence	this {@code Synapse}'s permanence
	 * @param index			this {@code Synapse}'s index
	 */
	public Synapse(Cell sourceCell, Segment segment, double permanence, int index) {
		this.sourceCell = sourceCell;
		this.segment = segment;
		this.permanence = permanence;
		this.index = index;
		
		sourceCell.addReceptorSynapse(this);
	}
	
	public double getPermanence() {
		return permanence;
	}
	
	public void setPermanence(double perm) {
		this.permanence = perm;
	}
	
	public Segment getSegment() {
		return segment;
	}
	
	public Cell getSourceCell() {
		return sourceCell;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "" + index;
	}
}
