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
		int connectedCount = 0;
		List<Synapse> synapses = c.getSynapses(this);
		for(Synapse s : synapses) {
			s.setPermanence(c, perms[s.getInputIndex()]);
			if(perms[s.getInputIndex()] >= c.getSynPermConnected()) {
				connectedCount++;
			}
		}
		c.getConnectedCounts()[index] = connectedCount;
	}
	
	/**
	 * Sets the permanences for each {@link Synapse} specified by the indexes
	 * passed in which identify the input vector indexes associated with the
	 * {@code Synapse}. The permanences passed in are understood to be in "sparse"
	 * format and therefore require the int array identify their corresponding
	 * indexes.
	 * 
	 * Note: This is the "sparse" version of this method.
	 * 
	 * @param c			the {@link Connections} memory
	 * @param perms		the floating point degree of connectedness
	 */
	public void setPermanences(Connections c, double[] perms, int[] inputIndexes) {
		pool.resetConnections();
		int connectedCount = 0;
		for(int i = 0;i < inputIndexes.length;i++) {
			pool.setPermanence(c, pool.getSynapseWithInput(inputIndexes[i]), perms[i]);
			if(perms[i] >= c.getSynPermConnected()) ++connectedCount;
		}
		c.getConnectedCounts()[index] = connectedCount;
	}
	
	/**
	 * Sets the input vector synapse indexes which are connected (>= synPermConnected)
	 * @param c
	 * @param connectedIndexes
	 */
	public void setConnectedSynapsesForTest(Connections c, int[] connectedIndexes) {
		Pool pool = createPool(c, connectedIndexes);
		c.getPotentialPools().set(index, pool);
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
