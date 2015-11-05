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

package org.numenta.nupic.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Pool;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.SparseBinaryMatrix;
import org.numenta.nupic.util.SparseMatrix;
import org.numenta.nupic.util.SparseObjectMatrix;

/**
 * Subclasses {@link SpatialPooler} to perform Prediction-Assisted CLA
 *
 * @author David Ray
 * @author Fergal Byrne
 *
 */
public class PASpatialPooler extends SpatialPooler {
    /**
     * This function determines each column's overlap with the current input
     * vector. The overlap of a column is the number of synapses for that column
     * that are connected (permanence value is greater than '_synPermConnected')
     * to input bits which are turned on. Overlap values that are lower than
     * the 'stimulusThreshold' are ignored. The implementation takes advantage of
     * the SpraseBinaryMatrix class to perform this calculation efficiently.
     *
     * @param c				the {@link Connections} memory encapsulation
     * @param inputVector   an input array of 0's and 1's that comprises the input to
     *                      the spatial pooler.
     * @return
     */
    public int[] calculateOverlap(Connections c, int[] inputVector) {
        int[] overlaps = new int[c.getNumColumns()];
        c.getConnectedCounts().rightVecSumAtNZ(inputVector, overlaps);
        overlaps = ArrayUtils.i_add(c.getPAOverlaps(), overlaps);
        ArrayUtils.lessThanXThanSetToY(overlaps, (int)c.getStimulusThreshold(), 0);
        return overlaps;
    }

}
