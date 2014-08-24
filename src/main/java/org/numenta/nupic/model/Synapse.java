/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.model;

import org.numenta.nupic.research.Connections;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Represents a connection with varying strength which when above 
 * a configured threshold represents a valid connection. This class
 * may hold state because its scope is limited to a given {@link Thread}'s
 * {@link Connections} object.
 * 
 * @author Chetan Surpur
 * @author David Ray
 * 
 * @see DistalDendrite
 * @see TemporalMemory.Connections
 */
public class Synapse {
	private Cell sourceCell;
	private DistalDendrite segment;
	private double permanence;
	private int index;
	
	/**
	 * Constructs a new {@code Synapse}
	 * 
	 * @param c				the connections state of the temporal memory
	 * @param sourceCell	the {@link Cell} which will activate this {@code Synapse}
	 * @param segment		the owning dendritic segment
	 * @param permanence	this {@code Synapse}'s permanence
	 * @param index			this {@code Synapse}'s index
	 */
	public Synapse(Connections c, Cell sourceCell, DistalDendrite segment, double permanence, int index) {
		this.sourceCell = sourceCell;
		this.segment = segment;
		this.permanence = permanence;
		this.index = index;
		
		sourceCell.addReceptorSynapse(c, this);
	}
	
	public double getPermanence() {
		return permanence;
	}
	
	public void setPermanence(double perm) {
		this.permanence = perm;
	}
	
	public DistalDendrite getSegment() {
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
