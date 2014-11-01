package org.numenta.nupic.model;

import java.util.List;

import org.numenta.nupic.Connections;

/**
 * For now simply a marker interface
 */
public abstract class Segment {
	/**
     * Creates and returns a newly created {@link Synapse} with the specified
     * source cell, permanence, and index.
     * 
     * IMPORTANT: 	For DistalDendrites, there is only one synapse per pool, so the
     * 				synapse's index doesn't really matter (in terms of tracking its
     * 				order within the pool. In that case, the index is a global counter
     * 				of all distal dendrite synapses.
     * 
     * 				For ProximalDendrites, there are many synapses within a pool, and in
     * 				that case, the index specifies the synapse's sequence order within
     * 				the pool object, and may be referenced by that index.
     * 
     * @param c             the connections state of the temporal memory
     * @param sourceCell    the source cell which will activate the new {@code Synapse}
     * @param pool		    the new {@link Synapse}'s pool for bound variables.
     * @param index         the new {@link Synapse}'s index.
     * @param inputIndex	the index of this {@link Synapse}'s input; be it a Cell or InputVector bit.
     * 
     * @return
     */
    public Synapse createSynapse(Connections c, List<Synapse> syns, Cell sourceCell, Pool pool, int index, int inputIndex) {
        Synapse s = new Synapse(c, sourceCell, this, pool, index, inputIndex);
        syns.add(s);
        return s;
    }
}
