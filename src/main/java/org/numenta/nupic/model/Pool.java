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

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.numenta.nupic.Connections;

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
	
	TObjectDoubleMap<Synapse> synapsePermanences = new TObjectDoubleHashMap<Synapse>();
	TIntArrayList synapseConnections = new TIntArrayList();
	Set<Synapse> synapseOrdering = new LinkedHashSet<Synapse>();
	
	TIntObjectMap<SynapsePair> connectionPerms = new TIntObjectHashMap<SynapsePair>();
	
	public Pool(int size) {
		this.size = size;
	}
	
	/**
	 * Returns the permanence value for the {@link Synapse} specified.
	 * 
	 * @param s	the Synapse
	 * @return	the permanence
	 */
	public double getPermanence(Synapse s) {
		return synapsePermanences.get(s);
	}
	
	/**
	 * Sets the specified  permanence value for the specified {@link Synapse}
	 * @param s
	 * @param permanence
	 */
	public void setPermanence(Connections c, Synapse s, double permanence) {
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
	
	/**
	 * Returns the {@link Synapse} connected to the specified input bit
	 * index.
	 * 
	 * @param inputIndex	the input vector connection's index.
	 * @return
	 */
	public Synapse getSynapseWithInput(int inputIndex) {
		return connectionPerms.get(inputIndex).getSynapse();
	}
	
	/**
	 * Returns an array of permanence values
	 * @return
	 */
	public double[] getSparsePermanences() {
		int i = 0;
		double[] retVal = new double[size];
		for(Synapse s : synapseOrdering) {
			retVal[i++] = connectionPerms.get(s.getInputIndex()).getPermanence();
		}
		return retVal;
	}
	
	/**
	 * Returns the a dense array representing the potential pool permanences
	 * 
	 * Note: Only called from tests for now...
	 * @param c
	 * @return
	 */
	public double[] getDensePermanences(Connections c) {
		double[] retVal = new double[c.getNumInputs()];
		Arrays.fill(retVal, 0);
		for(int inputIndex : connectionPerms.keys()) {
			retVal[inputIndex] = connectionPerms.get(inputIndex).getPermanence();
		}
		return retVal;
	}
	
	/**
	 * Returns an array of input bit indexes.
	 * @return
	 */
	public int[] getSparseConnections() {
		TIntList l = new TIntArrayList(connectionPerms.keys());
		l.reverse();
		return l.toArray();
	}
	
	/**
	 * Returns the a dense array representing the potential pool bits
	 * with the connected bits set to 1. 
	 * 
	 * Note: Only called from tests for now...
	 * @param c
	 * @return
	 */
	public int[] getDenseConnections(Connections c) {
		int[] retVal = new int[c.getNumInputs()];
		Arrays.fill(retVal, 0);
		for(int inputIndex : synapseConnections.toArray()) {
			retVal[inputIndex] = 1;
		}
		return retVal;
	}
	
	/**
	 * Used internally to associated a {@link Synapse} with its current
	 * permanence value.
	 * 
	 * @author David Ray
	 */
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
