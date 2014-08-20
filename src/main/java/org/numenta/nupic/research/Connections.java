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
package org.numenta.nupic.research;

import java.util.ArrayList;
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
 * Represents the definition of the interconnected structural state of the
 * {@link TemporalMemory} as well as the state of all support structures 
 * (i.e. Cells, Columns, Segments, Synapses etc.)
 */
public class Connections {
	protected Set<Cell> activeCells = new LinkedHashSet<Cell>();
	protected Set<Cell> winnerCells = new LinkedHashSet<Cell>();
	protected Set<Cell> predictiveCells = new LinkedHashSet<Cell>();
	protected Set<Column> predictedColumns = new LinkedHashSet<Column>();
	protected Set<Segment> activeSegments = new LinkedHashSet<Segment>();
	protected Set<Segment> learningSegments = new LinkedHashSet<Segment>();
	protected Map<Segment, Set<Synapse>> activeSynapsesForSegment = new LinkedHashMap<Segment, Set<Synapse>>();
	
	///////////////////////   Structural element state /////////////////////
	/** Reverse mapping from source cell to {@link Synapse} */
	protected Map<Cell, Set<Synapse>> receptorSynapses;
	protected Map<Cell, List<Segment>> segments;
	protected Map<Segment, List<Synapse>> synapses;
	
	/** Helps index each new Segment */
	protected int segmentCounter = 0;
	/** Helps index each new Synapse */
	protected int synapseCounter = 0;
	/** The default random number seed */
	protected int seed = 42;
	/** The random number generator */
	protected Random random = new Random(seed);
	
	
	
	/**
	 * Constructs a new {@code Connections} object. Use
	 * 
	 */
	public Connections() {}
	
	public void clear() {
		activeCells.clear();
		winnerCells.clear();
		predictiveCells.clear();
		predictedColumns.clear();
		activeSegments.clear();
		learningSegments.clear();
		activeSynapsesForSegment.clear();
	}
	
	/**
	 * Sets the seed used for the internal random number generator.
	 * If the generator has been instantiated, this method will initialize
	 * a new random generator with the specified seed.
	 * 
	 * @param seed
	 */
	public void setSeed(int seed) {
		random = new Random(seed);
	}
	
	/**
	 * Returns the configured random number seed
	 * @return
	 */
	public int seed() {
		return seed;
	}

	/**
	 * Returns the thread specific {@link Random} number generator.
	 * @return
	 */
	public Random random() {
		return random;
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
	
	/**
	 * Returns the mapping of {@link Cell}s to their reverse mapped 
	 * {@link Synapse}s.
	 * 
	 * @param cell		the {@link Cell} used as a key.
	 * @return			the mapping of {@link Cell}s to their reverse mapped 
	 * 					{@link Synapse}s.	
	 */
	public Set<Synapse> receptorSynapses(Cell cell) {
		if(cell == null) {
			throw new IllegalArgumentException("Cell was null");
		}
		
		if(receptorSynapses == null) {
			receptorSynapses = new LinkedHashMap<Cell, Set<Synapse>>();
		}
		
		Set<Synapse> retVal = null;
		if((retVal = receptorSynapses.get(cell)) == null) {
			receptorSynapses.put(cell, retVal = new LinkedHashSet<Synapse>());
		}
		
		return retVal;
	}
	
	/**
	 * Returns the mapping of {@link Cell}s to their {@link Segment}s.
	 * 
	 * @param cell		the {@link Cell} used as a key.
	 * @return			the mapping of {@link Cell}s to their {@link Segment}s.
	 */
	public List<Segment> segments(Cell cell) {
		if(cell == null) {
			throw new IllegalArgumentException("Cell was null");
		}
		
		if(segments == null) {
			segments = new LinkedHashMap<Cell, List<Segment>>();
		}
		
		List<Segment> retVal = null;
		if((retVal = segments.get(cell)) == null) {
			segments.put(cell, retVal = new ArrayList<Segment>());
		}
		
		return retVal;
	}
	
	/**
	 * Returns the mapping of {@link Segment}s to their {@link Synapse}s.
	 * 
	 * @param segment	the {@link Segment} used as a key.
	 * @return			the mapping of {@link Segment}s to their {@link Synapse}s.
	 */
	public List<Synapse> synapses(Segment segment) {
		if(segment == null) {
			throw new IllegalArgumentException("Segment was null");
		}
		
		if(synapses == null) {
			synapses = new LinkedHashMap<Segment, List<Synapse>>();
		}
		
		List<Synapse> retVal = null;
		if((retVal = synapses.get(segment)) == null) {
			synapses.put(segment, retVal = new ArrayList<Synapse>());
		}
		
		return retVal;
	}
}
