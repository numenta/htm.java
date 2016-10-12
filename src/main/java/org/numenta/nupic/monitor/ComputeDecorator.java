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
package org.numenta.nupic.monitor;

import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.Connections;

/**
 * Decorator interface for main algorithms 
 * 
 * @author cogmission
 */
public interface ComputeDecorator {
    /**
     * Feeds input record through TM, performing inferencing and learning
     * 
     * @param connections       the connection memory
     * @param activeColumns     direct activated column input
     * @param learn             learning mode flag
     * @return                  {@link ComputeCycle} container for one cycle of inference values.
     */
    public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn);
    /**
     * Called to start the input of a new sequence, and
     * reset the sequence state of the TM.
     * 
     * @param   connections   the Connections state of the temporal memory
     */
    public void reset(Connections connections);
}
