package org.numenta.nupic.monitor;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;


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
