package org.numenta.nupic.model;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Arrays;

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
	
	double[] synapsePermanences;
	TIntArrayList synapseConnections = new TIntArrayList();
	TObjectIntMap<Synapse> synapseIndexes = new TObjectIntHashMap<Synapse>();
	
	TIntObjectMap<TObjectDoubleMap<Synapse>> connectionPerms = new TIntObjectHashMap<TObjectDoubleMap<Synapse>>();
	
	public Pool(int size) {
		this.size = size;
		synapsePermanences = new double[size];
	}
	
	/**
	 * Returns the permanence value for the {@link Synapse} specified.
	 * 
	 * @param s	the Synapse
	 * @return	the permanence
	 */
	public double getPermanence(Synapse s) {
		return synapsePermanences[synapseIndexes.get(s)];
	}
	
	/**
	 * Adds the specified  permanence value for the specified {@link Synapse}
	 * @param s
	 * @param permanence
	 */
	public void addPermanence(Synapse s, double permanence) {
		setPermanence(s, permanence);
	}
	
	/**
	 * Sets the specified  permanence value for the specified {@link Synapse}
	 * @param s
	 * @param permanence
	 */
	public void setPermanence(Synapse s, double permanence) {
		if(!synapseIndexes.containsKey(s)) {
			synapseIndexes.put(s, synapseIndexes.size());
		}
		synapsePermanences[synapseIndexes.get(s)] = permanence;
//		TObjectDoubleMap<Synapse> synPerm = null;
//		if((synPerm = connectionPerms.get(s.getInputIndex())) == null) {
//			connectionPerms.put(s.getInputIndex(), synPerm = new TObjectDoubleHashMap<Synapse>());
//		}
//		synPerm.put(s, permanence);
	}
	
	/**
	 * Adds the input bit index for the {@link Synapse} specified by 
	 * the synapseIndex.
	 * 
	 * @param connection	the input bit index
	 */
	public void addConnection(int connection) {
		synapseConnections.add(connection);
	}
	
	/**
	 * Sets the input bit index for the {@link Synapse} specified
	 * 
	 * @param s				the Synapse being connected
	 * @param connection	the input bit index
	 */
	public void setConnection(Synapse s, int connection) {
		synapseConnections.set(synapseIndexes.get(s), connection);
	}
	
	/**
	 * Updates the entire pool with the indexes currently qualifying
	 * as connected.
	 * 
	 * @param connections
	 */
	public void setConnections(int[] connections) {
		synapseConnections.clear();
		synapseConnections.addAll(connections);
	}
	
	/**
	 * Clears the indexes of connected synapses in preparation
	 * for an entire update.
	 */
	public void clearConnections() {
		synapseConnections.clear();
	}
	
	/**
	 * Returns a count of the {@link Synapse}s whose permanence is
	 * greater than or equal to the threshold value specified.
	 * 
	 * @param threshold		the comparison value
	 * @return	the count
	 */
	public int getConnectedCount(double threshold) {
		int count = 0;
		for(double d : synapsePermanences) {
			count += d >= threshold ? 1 : 0;
		}
		return count;
	}
	
	/**
	 * Returns an array of permanence values
	 * @return
	 */
	public double[] getPermanences() {
		return synapsePermanences;
	}
	
	/**
	 * Returns an array of input bit indexes.
	 * @return
	 */
	public int[] getSparseConnections() {
		return synapseConnections.toArray();
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
		for(int i = 0;i < size;i++) {
			if(synapsePermanences[i] >= c.getSynPermConnected()) {
				retVal[i] = 1;
			}
		}
		return retVal;
	}
}
