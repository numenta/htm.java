package org.numenta.nupic.monitor;

import java.util.HashMap;
import java.util.Map;

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.monitor.mixin.TemporalMemoryMonitorMixin;
import org.numenta.nupic.monitor.mixin.Trace;


public class MonitoredTemporalMemory implements ComputeDecorator, TemporalMemoryMonitorMixin {
    private ComputeDecorator decorator;
    
    private Connections connections;
    
    private Map<String, Trace<?>> mmTraces = new HashMap<>();
    private Map<String, Map<String, ?>> mmData = new HashMap<>();
    
    private String mmName;
    
    private boolean mmResetActive;
    private boolean transitionTracesStale = true;
    
    public MonitoredTemporalMemory(ComputeDecorator decorator, Connections cnx) {
        this.decorator = decorator;
        this.mmResetActive = true;
        this.connections = cnx;
        
        mmClearHistory();
    }
    
    
    //////////////////////////////////////////////////////////////
    //         Mixin Virtual Extension Methods                  //
    //////////////////////////////////////////////////////////////
    
    @SuppressWarnings("unchecked")
    @Override
    public ComputeDecorator getMonitor() {
        return decorator;
    }
    
    @Override
    public Connections getConnections() {
        return connections;
    }

    @Override
    public Map<String, Trace<?>> getTraceMap() {
        return mmTraces;
    }
    
    @Override
    public Map<String, Map<String, ?>> getDataMap() {
        return mmData;
    }
    
    @Override
    public boolean resetActive() {
        return mmResetActive;
    }
    
    @Override
    public void setResetActive(boolean b) {
        this.mmResetActive = b;
    }
    
    /**
     * Returns the flag indicating whether the current traces 
     * are stale and need to be recomputed, or not.
     * 
     * @return
     */
    @Override
    public boolean transitionTracesStale() {
        return transitionTracesStale;
    }
    
    /**
     * Sets the flag indicating whether the current traces 
     * are stale and need to be recomputed, or not.
     * 
     * @param b
     */
    @Override
    public void setTransitionTracesStale(boolean b) {
        this.transitionTracesStale = b;
    }
    
    
    //////////////////////////////////////////////////////////////
    //                   Decorator Overrides                    //
    //////////////////////////////////////////////////////////////
    
    @Override
    public ComputeCycle compute(Connections connections, int[] activeColumns, boolean learn) {
        return compute(connections, activeColumns, learn);
    }

    @Override
    public void reset(Connections connections) {
        decorator.reset(connections);
    }


    @Override
    public String mmGetName() {
        return mmName;
    }    
}
