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

import org.numenta.nupic.Connections;

/**
 * Base class which handles the creation of {@link Synapse}s on behalf of
 * inheriting class types.
 * 
 * @author David Ray
 * @see DistalDendrite
 * @see ProximalDendrite
 */
public abstract class Segment {
    protected int index;
    
    /**
     * Returns this {@link ProximalDendrite}'s index.
     * @return
     */
    public int getIndex() {
        return index;
    }

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
     * @param inputIndex	the index of this {@link Synapse}'s input (source object); be it a Cell or InputVector bit.
     * 
     * @return
     */
    public Synapse createSynapse(Connections c, List<Synapse> syns, Cell sourceCell, Pool pool, int index, int inputIndex) {
        Synapse s = new Synapse(c, sourceCell, this, pool, index, inputIndex);
        syns.add(s);
        return s;
    }
}
