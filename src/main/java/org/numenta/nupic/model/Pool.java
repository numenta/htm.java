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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import org.numenta.nupic.Connections;
import org.numenta.nupic.util.ArrayUtils;

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

    /** Allows fast removal of connected synapse indexes. */
    private TIntHashSet synapseConnections = new TIntHashSet();
    /** 
     * Indexed according to the source Input Vector Bit (for ProximalDendrites),
     * and source cell (for DistalDendrites).
     */
    private TIntObjectMap<Synapse> synapsesBySourceIndex = new TIntObjectHashMap<>();

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
        return synapsesBySourceIndex.get(s.getInputIndex()).getPermanence();
    }

    /**
     * Sets the specified  permanence value for the specified {@link Synapse}
     * @param s
     * @param permanence
     */
    public void setPermanence(Connections c, Synapse s, double permanence) {
        updatePool(c, s, permanence);
        s.setPermanence(c, permanence);
    }

    /**
     * Updates this {@code Pool}'s store of permanences for the specified {@link Synapse}
     * @param c				the connections memory
     * @param s				the synapse who's permanence is recorded
     * @param permanence	the permanence value to record
     */
    public void updatePool(Connections c, Synapse s, double permanence) {
        int inputIndex = s.getInputIndex();
        if(synapsesBySourceIndex.get(inputIndex) == null) {
            synapsesBySourceIndex.put(inputIndex, s);
        }
        if(permanence > c.getSynPermConnected()) {
            synapseConnections.add(inputIndex);
        }else {
            synapseConnections.remove(inputIndex);
        }
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
        return synapsesBySourceIndex.get(inputIndex);
    }

    /**
     * Returns an array of permanence values
     * @return
     */
    public double[] getSparsePermanences() {
        double[] retVal = new double[size];
        int[] keys = synapsesBySourceIndex.keys();
        for(int x = 0, j = size - 1;x < size;x++, j--) {
            retVal[j] = synapsesBySourceIndex.get(keys[x]).getPermanence();
        }

        return retVal;
    }

    /**
     * Returns a dense array representing the potential pool permanences
     * 
     * Note: Only called from tests for now...
     * @param c
     * @return
     */
    public double[] getDensePermanences(Connections c) {
        double[] retVal = new double[c.getNumInputs()];
        int[] keys = synapsesBySourceIndex.keys();
        for(int inputIndex : keys) {
            retVal[inputIndex] = synapsesBySourceIndex.get(inputIndex).getPermanence();
        }
        return retVal;
    }

    /**
     * Returns an array of input bit indexes indicating the index of the source. 
     * (input vector bit or lateral cell)
     * @return the sparse array
     */
    public int[] getSparseConnections() {
        int[] keys = ArrayUtils.reverse(synapsesBySourceIndex.keys());
        return keys;
    }

    /**
     * Returns a dense array representing the potential pool bits
     * with the connected bits set to 1. 
     * 
     * Note: Only called from tests for now...
     * @param c
     * @return
     */
    public int[] getDenseConnections(Connections c) {
        int[] retVal = new int[c.getNumInputs()];
        for(int inputIndex : synapseConnections.toArray()) {
            retVal[inputIndex] = 1;
        }
        return retVal;
    }
}
