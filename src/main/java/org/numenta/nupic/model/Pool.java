package org.numenta.nupic.model;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.numenta.nupic.research.Connections;

/**
 * Convenience container for "bound" {@link Synapse} values
 * which can be dereferenced from both a Synapse and the 
 * {@link Connections} object. All Synapses will have a reference
 * to a {@code Pool} to retrieve relevant values. In addition, that
 * same pool can be referenced from the Connections object externally
 * which will update the Synapse's internal reference.
 * 
 * @author David Ray
 * @see Synapse
 * @see Connections
 */
public class Pool {
	int size;
	
	//double[] synapsePermanences;
	TObjectDoubleMap<Synapse> synapsePermanences = new TObjectDoubleHashMap<Synapse>();
	TIntArrayList synapseConnections = new TIntArrayList();
	Set<Synapse> synapseOrdering = new LinkedHashSet<Synapse>();
	
	TIntObjectMap<SynapsePair> connectionPerms = new TIntObjectHashMap<SynapsePair>();
	
	public Pool(int size) {
		this.size = size;
		//synapsePermanences = new double[size];
	}
	
	/**
	 * Returns the permanence value for the {@link Synapse} specified.
	 * 
	 * @param s	the Synapse
	 * @return	the permanence
	 */
	public double getPermanence(Synapse s) {
		//return synapsePermanences[synapseIndexes.get(s)];
		return synapsePermanences.get(s);
	}
	
	/**
	 * Sets the specified  permanence value for the specified {@link Synapse}
	 * @param s
	 * @param permanence
	 */
	public void setPermanence(Connections c, Synapse s, double permanence) {
//		if(!synapseIndexes.containsKey(s)) {
//			synapseIndexes.put(s, synapseIndexes.size());
//		}
//		synapsePermanences[synapseIndexes.get(s)] = permanence;
		SynapsePair synPerm = null;
		if((synPerm = connectionPerms.get(s.getInputIndex())) == null) {
			connectionPerms.put(s.getInputIndex(), synPerm = new SynapsePair(s, permanence));
			synapseOrdering.add(s);
		}
		if(permanence > c.getSynPermConnected()) {
			synapseConnections.add(s.getInputIndex());
		}
		synapsePermanences.put(s, permanence);
		synPerm.setPermanence(permanence);
	}
	
	/**
	 * Resets the current connections in preparation for new permanence
	 * adjustments.
	 */
	public void resetConnections() {
		synapseConnections.clear();
	}
	
	public Synapse getSynapseWithInput(int inputIndex) {
		return connectionPerms.get(inputIndex).getSynapse();
	}
	
//	/**
//	 * Adds the input bit index for the {@link Synapse} specified by 
//	 * the synapseIndex.
//	 * 
//	 * @param connection	the input bit index
//	 */
//	public void addConnection(int connection) {
//		synapseConnections.add(connection);
//	}
//	
//	/**
//	 * Updates the entire pool with the indexes currently qualifying
//	 * as connected. (permanence >= synPermConnected)
//	 * 
//	 * @param connections
//	 */
//	public void setConnections(Connections c, ProximalDendrite pd, int[] connections) {
//		synapseConnections.clear();
//		synapseConnections.addAll(connections);
//		
//		connectionPerms.clear();
//		for(int i = 0;i < connections.length;i++) { 
//			setPermanence(pd.createSynapse(c, c.getSynapses(pd), null, this, -1, connections[i]), c.getSynPermConnected());
//		}
//	}
	
	/**
	 * Returns an array of permanence values
	 * @return
	 */
	public double[] getPermanences() {
		//return synapsePermanences;
		int i = 0;
		double[] retVal = new double[size];
		for(Synapse s : synapseOrdering) {
			retVal[i++] = connectionPerms.get(s.getInputIndex()).getPermanence();
		}
		return retVal;
	}
	
	/**
	 * Returns an array of input bit indexes.
	 * @return
	 */
	public int[] getSparseConnections() {
		//return synapseConnections.toArray();
		return connectionPerms.keys();
	}
	
	/**
	 * Returns the a dense array representing the potential pool bits
	 * with the connected bits set to 1.
	 * @param c
	 * @return
	 */
	public int[] getDenseConnections(Connections c) {
		int[] retVal = new int[size];
		Arrays.fill(retVal, 0);
//		for(int i = 0;i < size;i++) {
//			if(synapsePermanences[i] >= c.getSynPermConnected()) {
//				retVal[i] = 1;
//			}
//		}
		
		for(int inputIndex : synapseConnections.toArray()) {
			retVal[inputIndex] = 1;
		}
		return retVal;
	}
	
	private class SynapsePair {
		private Synapse synapse;
		private double permanence;
		
		public SynapsePair(Synapse s, double p) {
			this.synapse = s;
			this.permanence = p;
		}
		
		public Synapse getSynapse() {
			return synapse;
		}
		
		public double getPermanence() {
			return permanence;
		}
		
		public void setPermanence(double permanence) {
			this.permanence = permanence;
		}
	}
}
