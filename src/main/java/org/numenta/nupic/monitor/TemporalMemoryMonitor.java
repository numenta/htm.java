package org.numenta.nupic.monitor;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.monitor.mixin.TemporalMemoryMonitorMixin;


public class TemporalMemoryMonitor implements ComputeDecorator, TemporalMemoryMonitorMixin {
    private ComputeDecorator decorator;
    
    public TemporalMemoryMonitor(ComputeDecorator decorator) {
        this.decorator = decorator;
    }
    
    @Override
    public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn) {
        return decorator.compute(connections, activeColumns, learn);
    }

    @Override
    public void reset(Connections connections) {
        decorator.reset(connections);
    }

}
