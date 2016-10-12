/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.model;

import java.util.stream.IntStream;

import org.numenta.nupic.util.ArrayUtils;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

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
public class Pool implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
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
        if(permanence >= c.getSynPermConnected()) {
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
    public int[] getSparsePotential() {
        return ArrayUtils.reverse(synapsesBySourceIndex.keys());
    }
    
    /**
     * Returns a dense binary array containing 1's where the input bits are part
     * of this pool.
     * @param c     the {@link Connections}
     * @return  dense binary array of member inputs
     */
    public int[] getDensePotential(Connections c) {
        return IntStream.range(0, c.getNumInputs())
            .map(i -> synapsesBySourceIndex.containsKey(i) ? 1 : 0)
            .toArray();
    }
    
    /**
     * Returns an binary array whose length is equal to the number of inputs;
     * and where 1's are set in the indexes of this pool's assigned bits.
     * 
     * @param   c   {@link Connections}
     * @return the sparse array
     */
    public int[] getDenseConnected(Connections c) {
        return IntStream.range(0, c.getNumInputs())
            .map(i -> synapseConnections.contains(i) ? 1 : 0)
            .toArray();
    }

    /**
     * Destroys any references this {@code Pool} maintains on behalf
     * of the specified {@link Synapse}
     * 
     * @param synapse
     */
    public void destroySynapse(Synapse synapse) {
        synapseConnections.remove(synapse.getInputIndex());
        synapsesBySourceIndex.remove(synapse.getInputIndex());
        if(synapse.getSegment() instanceof DistalDendrite) {
            destroy();
        }
    }
    
    /**
     * Clears the state of this {@code Pool}
     */
    public void destroy() {
        synapseConnections.clear();
        synapsesBySourceIndex.clear();
        synapseConnections = null;
        synapsesBySourceIndex = null;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + size;
        result = prime * result + ((synapseConnections == null) ? 0 : synapseConnections.toString().hashCode());
        result = prime * result + ((synapsesBySourceIndex == null) ? 0 : synapsesBySourceIndex.toString().hashCode());
        return result;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Pool other = (Pool)obj;
        if(size != other.size)
            return false;
        if(synapseConnections == null) {
            if(other.synapseConnections != null)
                return false;
        } else if((!synapseConnections.containsAll(other.synapseConnections) || 
            !other.synapseConnections.containsAll(synapseConnections)))
                return false;
        if(synapsesBySourceIndex == null) {
            if(other.synapsesBySourceIndex != null)
                return false;
        } else if(!synapsesBySourceIndex.toString().equals(other.synapsesBySourceIndex.toString()))
            return false;
        return true;
    }
}
