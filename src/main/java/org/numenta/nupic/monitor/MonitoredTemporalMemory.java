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

import java.util.HashMap;
import java.util.Map;

import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.monitor.mixin.TemporalMemoryMonitorMixin;
import org.numenta.nupic.monitor.mixin.Trace;

/**
 * This class is an example of building a test class for the "MonitorMixin"
 * framework. This class is referenced as would the original class being tested
 * would be except that it has "mixins" (traits really) which provide extra
 * functionality for monitoring behavior and reporting.
 * 
 * @author cogmission
 *
 */
public class MonitoredTemporalMemory implements ComputeDecorator, TemporalMemoryMonitorMixin {
    private ComputeDecorator decorator;
    
    private Connections connections;
    
    private Map<String, Trace<?>> mmTraces = new HashMap<>();
    private Map<String, Map<String, ?>> mmData = new HashMap<>();
    
    private String mmName;
    
    private boolean mmResetActive;
    private boolean transitionTracesStale = true;
    
    
    /**
     * Constructs a new {@code MonitoredTemporalMemory}
     * 
     * @param decorator     The decorator class
     * @param cnx           the {@link Connections} object.
     */
    public MonitoredTemporalMemory(ComputeDecorator decorator, Connections cnx) {
        this.decorator = decorator;
        this.mmResetActive = true;
        this.connections = cnx;
        
        mmClearHistory();
    }
    
    
    //////////////////////////////////////////////////////////////
    //         Mixin Virtual Extension Methods                  //
    //////////////////////////////////////////////////////////////
    
    /**
     * Returns the original class which is being tested. In this 
     * case it is the {@link TemporalMemory}
     */
    @SuppressWarnings("unchecked")
    @Override
    public ComputeDecorator getMonitor() {
        return decorator;
    }
    
    /**
     * Returns the {@link Connections} object
     */
    @Override
    public Connections getConnections() {
        return connections;
    }

    /**
     * The map of the entire {@link Trace} data for this mixin testing framework.
     */
    @Override
    public Map<String, Trace<?>> getTraceMap() {
        return mmTraces;
    }
    
    /**
     * The map of the {@link Metric} data for this testing framework.
     */
    @Override
    public Map<String, Map<String, ?>> getDataMap() {
        return mmData;
    }
    
    /**
     * Flag which tells the mixin what state we're in.
     */
    @Override
    public boolean resetActive() {
        return mmResetActive;
    }
    
    /**
     * Sets the Flag which indicates to the mixin what state we're in.
     */
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
