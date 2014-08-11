package org.numenta.nupic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.research.Connections;

/**
 * Represents a proximal or distal dendritic segment.
 * Segments are owned by {@link Cell}s and in turn own {@link Synapse}s
 * which are obversely connected to by a "source cell", which is the {@link Cell}
 * which will activate a given {@link Synapse} owned by this {@code Segment}.
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class Segment {
	private Cell cell;
	private int index;
	
	/**
	 * Constructs a new {@code Segment} object with the specified
	 * owner {@link Cell} and the specified index.
	 * 
	 * @param cell		the owner
	 * @param index		this {@code Segment}'s index.
	 */
	public Segment(Cell cell, int index) {
		this.cell = cell;
		this.index = index;
	}
	
	/**
	 * Returns the owner {@link Cell} 
	 * @return
	 */
	public Cell getParentCell() {
		return cell;
	}
	
	/**
	 * Creates and returns a newly created {@link Synapse} with the specified
	 * source cell, permanence, and index.
	 * 
	 * @param c				the connections state of the temporal memory
	 * @param sourceCell	the source cell which will activate the new {@code Synapse}
	 * @param permanence	the new {@link Synapse}'s initial permanence.
	 * @param index			the new {@link Synapse}'s index.
	 * @return
	 */
	public Synapse createSynapse(Connections c, Cell sourceCell, double permanence, int index) {
		Synapse s = new Synapse(c, sourceCell, this, permanence, index);
		c.synapses(this).add(s);
		return s;
	}
	
	/**
	 * Returns all {@link Synapse}s
	 * 
	 * @param	c	the connections state of the temporal memory
	 * @return
	 */
	public List<Synapse> getAllSynapses(Connections c) {
		return c.synapses(this);
	}
	
	/**
	 * Returns the synapses on a segment that are active due to lateral input
     * from active cells.
     * 
	 * @param activeSynapsesForSegment
	 * @param permanenceThreashold
	 * @return
	 */
	public Set<Synapse> getConnectedActiveSynapses(Map<Segment, Set<Synapse>> activeSynapsesForSegment, double permanenceThreashold) {
		Set<Synapse> connectedSynapses = new LinkedHashSet<Synapse>();
		
		if(!activeSynapsesForSegment.containsKey(this)) {
			return connectedSynapses;
		}
		
		for(Synapse s : activeSynapsesForSegment.get(this)) {
			if(s.getPermanence() >= permanenceThreashold) {
				connectedSynapses.add(s);
			}
		}
		return connectedSynapses;
	}
	
	/**
	 * Called for learning {@code Segment}s so that they may
	 * adjust the permanences of their synapses.
	 * 
	 * @param c						the connections state of the temporal memory
	 * @param activeSynapses		a set of active synapses owned by this {@code Segment} which
	 * 								will have their permanences increased. All others will have their
	 * 								permanences decreased.
	 * @param permanenceIncrement	the increment by which permanences are increased.
	 * @param permanenceDecrement	the increment by which permanences are decreased.
	 */
	public void adaptSegment(Connections c, Set<Synapse> activeSynapses, double permanenceIncrement, double permanenceDecrement) {
		for(Synapse synapse : c.synapses(this)) {
			double permanence = synapse.getPermanence();
			if(activeSynapses.contains(synapse)) {
				permanence += permanenceIncrement;
			}else{
				permanence -= permanenceDecrement;
			}
			
			permanence = Math.max(0, Math.min(1.0, permanence));
			
			synapse.setPermanence(permanence);
		}
	}
	
	/**
	 * Returns a {@link Set} of previous winner {@link Cell}s which aren't already attached to any
	 * {@link Synapse}s owned by this {@code Segment}
	 * 
	 * @param	c				the connections state of the temporal memory
	 * @param numPickCells		the number of possible cells this segment may designate
	 * @param prevWinners		the set of previous winner cells
	 * @param random			the random number generator
	 * @return					a {@link Set} of previous winner {@link Cell}s which aren't already attached to any
	 * 							{@link Synapse}s owned by this {@code Segment}
	 */
	public Set<Cell> pickCellsToLearnOn(Connections c, int numPickCells, Set<Cell> prevWinners, Random random) {
		//Create a list of cells that aren't already synapsed to this segment
		Set<Cell> candidates = new LinkedHashSet<Cell>(prevWinners);
		for(Synapse synapse : c.synapses(this)) {
			Cell sourceCell = synapse.getSourceCell();
			if(candidates.contains(sourceCell)) {
				candidates.remove(sourceCell);
			}
		}
		
		numPickCells = Math.min(numPickCells, candidates.size());
		List<Cell> cands = new ArrayList<Cell>(candidates);
		Collections.sort(cands);
		
		Set<Cell> cells = new LinkedHashSet<Cell>();
		for(int x = 0;x < numPickCells;x++) {
			int i = random.nextInt(cands.size());
			cells.add(cands.get(i));
			cands.remove(i);
		}
		
		return cells;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "" + index;
	}
}
