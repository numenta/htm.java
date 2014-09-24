package org.numenta.nupic.model;

import java.util.List;

import org.numenta.nupic.research.Connections;

public class ProximalDendrite extends Segment {
	private int index;
	private Pool pool;
	
	/**
	 * 
	  * @param index     this {@code ProximalDendrite}'s index.
	 */
	public ProximalDendrite(int index) {
		this.index = index;
	}
	
	/**
	 * Creates the pool of {@link Synapse}s representing the connection
	 * to the input vector.
	 * 
	 * @param c					the {@link Connections} memory
	 * @param inputIndexes		indexes specifying the input vector bit
	 */
	public Pool createPool(Connections c, int[] inputIndexes) {
		pool = new Pool(inputIndexes.length);
		for(int i = 0;i < inputIndexes.length;i++) {
			pool.addConnection(inputIndexes[i]);
			pool.addPermanence(createSynapse(c, c.getSynapses(this), null, pool, i), 0);
		}
		return pool;
	}
	
	/**
	 * Returns this {@link ProximalDendrite}'s index.
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Sets the permanences for each {@link Synapse}
	 * 
	 * @param c			the {@link Connections} memory
	 * @param perms		the floating point degree of connectedness
	 */
	public void setPermanences(Connections c, double[] perms) {
		List<Synapse> synapses = c.getSynapses(this);
		int i = 0;
		for(Synapse s : synapses) {
			s.setPermanence(perms[i++]);
		}
	}
}
