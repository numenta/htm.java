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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.Connections;

/**
 * Represents a proximal or distal dendritic segment. Segments are owned by
 * {@link Cell}s and in turn own {@link Synapse}s which are obversely connected
 * to by a "source cell", which is the {@link Cell} which will activate a given
 * {@link Synapse} owned by this {@code Segment}.
 * 
 * @author Chetan Surpur
 * @author David Ray
 */
public class DistalDendrite extends Segment {
    private static final double EPSILON = 0.0000001;
    
    private Cell cell;
    
    /**
     * Constructs a new {@code Segment} object with the specified owner
     * {@link Cell} and the specified index.
     * 
     * @param cell
     *            the owner
     * @param index
     *            this {@code Segment}'s index.
     */
    public DistalDendrite(Cell cell, int index) {
        this.cell = cell;
        this.index = index;
    }

    /**
     * Returns the owner {@link Cell}
     * 
     * @return
     */
    public Cell getParentCell() {
        return cell;
    }

    /**
     * Creates and returns a newly created {@link Synapse} with the specified
     * source cell, permanence, and index.
     * 
     * @param c             the connections state of the temporal memory
     * @param sourceCell    the source cell which will activate the new {@code Synapse}
     * @param permanence    the new {@link Synapse}'s initial permanence.
     * @param index         the new {@link Synapse}'s index.
     * 
     * @return
     */
    public Synapse createSynapse(Connections c, Cell sourceCell, double permanence) {
        Pool pool = new Pool(1);
        Synapse s = super.createSynapse(c, c.getSynapses(this), sourceCell, pool, c.incrementSynapses(), sourceCell.getIndex());
        pool.setPermanence(c, s, permanence);
        return s;
    }

    /**
     * Returns all {@link Synapse}s
     * 
     * @param c     the connections state of the temporal memory
     * @return
     */
    public List<Synapse> getAllSynapses(Connections c) {
        return c.getSynapses(this);
    }

    /**
     * Returns the synapses on a segment that are active due to lateral input
     * from active cells.
     * 
     * @param c                 the layer connectivity
     * @param activeCells       the active cells
     * @return  Set of {@link Synapse}s connected to active presynaptic cells.
     */
    public Set<Synapse> getActiveSynapses(Connections c, Set<Cell> activeCells) {
        Set<Synapse> synapses = new LinkedHashSet<>();
        
        for(Synapse synapse : c.getSynapses(this)) {
            if(activeCells.contains(synapse.getPresynapticCell())) {
                synapses.add(synapse);
            }
        }
        
        return synapses;
    }

    /**
     * Called for learning {@code Segment}s so that they may adjust the
     * permanences of their synapses.
     * 
     * @param c                         the connections state of the temporal memory
     * @param activeSynapses            a set of active synapses owned by this {@code Segment} which
     *                                  will have their permanences increased. All others will have
     *                                  their permanences decreased.
     * @param permanenceIncrement       the increment by which permanences are increased.
     * @param permanenceDecrement       the increment by which permanences are decreased.
     */
    public void adaptSegment(Connections c, Set<Synapse> activeSynapses, double permanenceIncrement, double permanenceDecrement) {
        List<Synapse> synapsesToDestroy = null;
        
        for(Synapse synapse : c.getSynapses(this)) {
            double permanence = synapse.getPermanence();
            if(activeSynapses.contains(synapse)) {
                permanence += permanenceIncrement;
            } else {
                permanence -= permanenceDecrement;
            }

            permanence = permanence < 0 ? 0 : permanence > 1.0 ? 1.0 : permanence;

            if(Math.abs(permanence) < EPSILON) {
                if(synapsesToDestroy == null) {
                    synapsesToDestroy = new ArrayList<>();
                }
                synapsesToDestroy.add(synapse);
            }else{
                synapse.setPermanence(c, permanence);
            }
        }
        
        if(synapsesToDestroy != null) {
            for(Synapse s : synapsesToDestroy) {
                s.destroy(c);
            }
        }
    }

    /**
     * Returns a {@link Set} of previous winner {@link Cell}s which aren't
     * already attached to any {@link Synapse}s owned by this {@code Segment}
     * 
     * @param c                 the connections state of the temporal memory
     * @param numPickCells      the number of possible cells this segment may designate
     * @param prevWinners       the set of previous winner cells
     * @param random            the random number generator
     * @return a {@link Set} of previous winner {@link Cell}s which aren't
     *         already attached to any {@link Synapse}s owned by this
     *         {@code Segment}
     */
    public Set<Cell> pickCellsToLearnOn(Connections c, int numPickCells, Set<Cell> prevWinners, Random random) {
        // Remove cells that are already synapsed on by this segment
        Set<Cell> candidates = new LinkedHashSet<Cell>(prevWinners);
        for(Synapse synapse : c.getSynapses(this)) {
            Cell sourceCell = synapse.getPresynapticCell();
            if(candidates.contains(sourceCell)) {
                candidates.remove(sourceCell);
            }
        }

        numPickCells = Math.min(numPickCells, candidates.size());
        List<Cell> cands = new ArrayList<Cell>(candidates);
        Collections.sort(cands);

        Set<Cell> cells = new LinkedHashSet<Cell>();
        for(int x = 0;x < numPickCells;x++) {
            int i = random.nextInt(cands.size());
            cells.add(cands.remove(i));
        }

        return cells;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "" + index;
    }
}
