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
package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.numenta.nupic.network.Layer;
import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.SDR;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.PASpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.NamedTuple;
import org.numenta.nupic.encoders.MultiEncoder;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

/**
 * Extension to Prediction-Assisted CLA
 * 
 * @author David Ray
 * @author Fergal Byrne
 */
public class PALayer<T> extends Layer<T> {

    public double paDepolarize = 2.0;

    public PALayer(Network n) {
	super(n);
    }
    public PALayer(Network n, Parameters p) {
        super(n, p);
    }
    public PALayer(String name, Network n, Parameters p) {
        super(name, n, p);
    }
    public PALayer(Parameters params, MultiEncoder e, SpatialPooler sp, TemporalMemory tm, Boolean autoCreateClassifiers, Anomaly a) {
        super(params, e, sp, tm, autoCreateClassifiers, a);
    }

    public double getPADepolarize() {
        return paDepolarize;
    }
    public void setPADepolarize(double pa) {
        paDepolarize = pa;
    }
    /**
     * Called internally to invoke the {@link TemporalMemory}
     * 
     * @param input
     *            the current input vector
     * @param mi
     *            the current input inference container
     * @return
     */
    protected int[] temporalInput(int[] input, ManualInput mi) {
        int[] sdr = super.temporalInput(input, mi);
        ComputeCycle cc = mi.computeCycle;
        if(spatialPooler != null && spatialPooler instanceof PASpatialPooler) {
            PASpatialPooler paSP = (PASpatialPooler)spatialPooler;
            int[] polarization = new int[connections.getNumColumns()];
            for(Cell cell : cc.predictiveCells) {
                Column column = cell.getColumn();
                polarization[column.getIndex()] += (int)paDepolarize;
            }
            paSP.setPAOverlaps(polarization);
        }
        return sdr;
    }
}
