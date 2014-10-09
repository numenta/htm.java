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
			//pool.addConnection(inputIndexes[i]);
			int synCount = c.getSynapseCount();
			pool.setPermanence(c, createSynapse(c, c.getSynapses(this), null, pool, synCount, inputIndexes[i]), 0);
			c.setSynapseCount(synCount + 1);
		}
		return pool;
	}
	
	public void clearSynapses(Connections c) {
		c.getSynapses(this).clear();
	}
	
	/**
	 * Returns this {@link ProximalDendrite}'s index.
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Sets the permanences for each {@link Synapse}. The number of synapses
	 * is set by the potentialPct variable which determines the number of input
	 * bits a given column will be "attached" to which is the same number as the
	 * number of {@link Synapse}s
	 * 
	 * @param c			the {@link Connections} memory
	 * @param perms		the floating point degree of connectedness
	 */
	public void setPermanences(Connections c, double[] perms) {
		pool.resetConnections();
		List<Synapse> synapses = c.getSynapses(this);
		int i = 0;
		for(Synapse s : synapses) {
			s.setPermanence(c, perms[i++]);
		}
	}
	
	/**
	 * Sets the input vector synapse indexes which are connected (>= synPermConnected)
	 * @param c
	 * @param connectedIndexes
	 */
	public void setConnectedSynapsesForTest(Connections c, int[] connectedIndexes) {
		Pool pool = createPool(c, connectedIndexes);
		c.getPotentialPools().set(index, pool);
		//c.getPotentialPools().getObject(index).setConnections(c, this, connectedIndexes);
	}
	
	/**
	 * Returns an array of synapse indexes as a dense binary array.
	 * @param c
	 * @return
	 */
	public int[] getConnectedSynapsesDense(Connections c) {
		return c.getPotentialPools().getObject(index).getDenseConnections(c);
	}
	
	/**
	 * Returns an sparse array of synapse indexes representing the connected bits.
	 * @param c
	 * @return
	 */
	public int[] getConnectedSynapsesSparse(Connections c) {
		return c.getPotentialPools().getObject(index).getSparseConnections();
	}
}
