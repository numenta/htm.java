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

package org.numenta.nupic.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.datagen.PatternMachine;
import org.numenta.nupic.datagen.SequenceMachine;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.monitor.MonitoredTemporalMemory;
import org.numenta.nupic.util.ArrayUtils;


public class AbstractTemporalMemoryTest {
    protected TemporalMemory temporalMemory;
    protected Parameters parameters;
    protected Connections connections;
    protected PatternMachine patternMachine;
    protected SequenceMachine sequenceMachine;
    
    protected MonitoredTemporalMemory tm;
    
    public void init(Parameters overrides, PatternMachine pm) {
        this.parameters = createTMParams(overrides);
        this.connections = new Connections();
        parameters.apply(connections);
        
        temporalMemory = new TemporalMemory();
        TemporalMemory.init(connections);
        tm = new MonitoredTemporalMemory(temporalMemory, connections);
        
        this.patternMachine = pm;
        this.sequenceMachine = new SequenceMachine(patternMachine);
    }
    
    /**
     * Creates {@link Parameters} for tests
     */
    protected Parameters createTMParams(Parameters overrides) {
        parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 100 });
        parameters.set(KEY.CELLS_PER_COLUMN, 1);
        parameters.set(KEY.INITIAL_PERMANENCE, 0.8);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.7);
        parameters.set(KEY.MIN_THRESHOLD, 11);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 11);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.4);
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.0);
        parameters.set(KEY.ACTIVATION_THRESHOLD, 11);
        
        if(overrides != null) {
            parameters.union(overrides);
        }
        
        return parameters;
    }
    
    public void feedTM(List<Set<Integer>> sequence, String label, boolean learn, int num) {
        List<Set<Integer>> repeatedSequence = new ArrayList<Set<Integer>>(sequence);
        if(num > 1) {
            for(int i = 1;i < num;i++) {
                repeatedSequence.addAll(sequence);
            }
        }
        
        tm.mmClearHistory();
        for(Set<Integer> pattern : repeatedSequence) {
            if(pattern == SequenceMachine.NONE) {
                tm.resetSequences(connections);
            }else{
                tm.compute(connections, ArrayUtils.toPrimitive(pattern.toArray(new Integer[0])), label, learn);
            }
        }
    }
    
}
