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

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.PASpatialPooler;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;

/**
 * Extension to Prediction-Assisted CLA
 * 
 * PALayer is a Layer which can contain a PASpatialPooler, in which case (via Connections) 
 * it prepopulates the paSP's overlap vector with a depolarisation value derived from its 
 * TM's predictive cells. PASpatialPooler adds this vector to the overlaps calculated from the 
 * feedforward input before doing inhibition. This change pre-biases the paSP to favour columns 
 * with predictive cells. 
 * 
 * Full details at http://arxiv.org/abs/1509.08255
 *
 * @author David Ray
 * @author Fergal Byrne
 */
public class PALayer<T> extends Layer<T> {

    /** Set to 0.0 to default to parent behavior */
    public double paDepolarize = 2.0;

    /**
     * Constructs a new {@code PALayer} which resides in the specified
     * {@link Network}
     * 
     * @param n     the parent {@link Network}
     */
    public PALayer(Network n) {
	      super(n);
    }
    
    /**
     * Constructs a new {@code PALayer} which resides in the specified
     * {@link Network} and uses the specified {@link Parameters}
     * 
     * @param n     the parent {@link Network}
     * @param p     the parameters object from which to obtain settings
     */
    public PALayer(Network n, Parameters p) {
        super(n, p);
    }
    
    /**
     * Constructs a new {@code PALayer} which resides in the specified
     * {@link Network} and uses the specified {@link Parameters}, with
     * the specified name.
     * 
     * @param name  the name specified
     * @param n     the parent {@link Network}
     * @param p     the parameters object from which to obtain settings
     */
    public PALayer(String name, Network n, Parameters p) {
        super(name, n, p);
    }
    
    /**
     * Manual method of creating a {@code Layer} and specifying its content.
     * 
     * @param params                    the parameters object from which to obtain settings
     * @param e                         an (optional) encoder providing input
     * @param sp                        an (optional) SpatialPooler
     * @param tm                        an (optional) {@link TemporalMemory}    
     * @param autoCreateClassifiers     flag indicating whether to create {@link CLAClassifier}s
     * @param a                         an (optional) {@link Anomaly} computer.
     */
    public PALayer(Parameters params, MultiEncoder e, SpatialPooler sp, TemporalMemory tm, Boolean autoCreateClassifiers, Anomaly a) {
        super(params, e, sp, tm, autoCreateClassifiers, a);
    }
    
    /**
     * Returns paDepolarize (predictive assist per cell) for this {@link PALayer}
     *
     * @return
     */
    public double getPADepolarize() {
        return paDepolarize;
    }
    
    /**
     * Sets paDepolarize {@code PALayer}
     *
     * @param pa
     */
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
            int[] polarization = new int[connections.getNumColumns()];
            for(Cell cell : cc.predictiveCells) {
                Column column = cell.getColumn();
                polarization[column.getIndex()] += (int)paDepolarize;
            }
            connections.setPAOverlaps(polarization);
        }
        return sdr;
    }
}
