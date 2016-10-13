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

package org.numenta.nupic.benchmarks;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.Connections;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public abstract class AbstractAlgorithmBenchmark {

    protected int[] SDR;
    protected ScalarEncoder encoder;
    protected SpatialPooler pooler;
    protected TemporalMemory temporalMemory;
    protected Connections memory;

    @Setup
    public void init() {
        SDR = new int[2048];

        //Layer components
        ScalarEncoder.Builder dayBuilder = ScalarEncoder.builder()
            .n(8)
            .w(3)
            .radius(1.0)
            .minVal(1.0)
            .maxVal(8)
            .periodic(true)
            .forced(true)
            .resolution(1);
        encoder = dayBuilder.build();
        pooler = new SpatialPooler();

        memory = new Connections();
        Parameters params = getParameters();
        params.apply(memory);

        pooler = new SpatialPooler();
        pooler.init(memory);

        temporalMemory = new TemporalMemory();
        TemporalMemory.init(memory);
    }

    /**
     * Create and return a {@link Parameters} object.
     * 
     * @return
     */
    protected Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        parameters.set(KEY.CELLS_PER_COLUMN, 32);

        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        
        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.4);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.5);
        parameters.set(KEY.MIN_THRESHOLD, 4);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 4);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);

        return parameters;
    }
}
