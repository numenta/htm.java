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

import java.util.List;

public class ProximalDendrite extends Segment implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private Pool pool;

    /**
     * 
     * @param index     this {@code ProximalDendrite}'s index.
     */
    public ProximalDendrite(int index) {
        super(index);
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
            int synCount = c.getProximalSynapseCount();
            pool.setPermanence(c, createSynapse(c, c.getSynapses(this), null, pool, synCount, inputIndexes[i]), 0);
            c.setProximalSynapseCount(synCount + 1);
        }
        return pool;
    }

    public void clearSynapses(Connections c) {
        c.getSynapses(this).clear();
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
        c.getConnectedCounts().clearStatistics(index);
        List<Synapse> synapses = c.getSynapses(this);
        for(Synapse s : synapses) {
            s.setPermanence(c, perms[s.getInputIndex()]);
            if(perms[s.getInputIndex()] >= c.getSynPermConnected()) {
                c.getConnectedCounts().set(1, index, s.getInputIndex());
            }
        }
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
        c.getConnectedCounts().clearStatistics(index);
        for(int i = 0;i < inputIndexes.length;i++) {
            pool.setPermanence(c, pool.getSynapseWithInput(inputIndexes[i]), perms[i]);
            if(perms[i] >= c.getSynPermConnected()) {
                c.getConnectedCounts().set(1, index, i);
            }
        }
    }

    /**
     * Sets the input vector synapse indexes which are connected (&gt;= synPermConnected)
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
        return c.getPotentialPools().get(index).getDenseConnected(c);
    }

    /**
     * Returns an sparse array of synapse indexes representing the connected bits.
     * @param c
     * @return
     */
    public int[] getConnectedSynapsesSparse(Connections c) {
        return c.getPotentialPools().get(index).getSparsePotential();
    }
}
