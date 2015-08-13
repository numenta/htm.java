package org.numenta.nupic.monitor.mixin;

import java.util.ArrayList;
import java.util.List;


/**
 * A record of the past data the algorithm has seen, with an entry for each
 * iteration.
 * 
 * Contains {@code Trace} classes used in monitor mixin framework.
 * 
 * @author Chetan Surpur
 * @author cogmission
 */
public abstract class Trace<T> {
    protected MonitorMixinBase monitor;
    protected String title;
    
    protected List<T> data;
    
    /**
     * Constructs a new {@code Trace}
     * @param monitor
     * @param title
     */
    public Trace(MonitorMixinBase monitor, String title) {
        this.monitor = monitor;
        this.title = title;
        
        data = new ArrayList<>();
    }
    
    /**
     * Returns the implementing mixin name if not null 
     * plus the configured title.
     * 
     * @return
     */
    public String prettyPrintTitle() {
        return monitor.mmGetName() != null ? 
            String.format("[%0s] %1s", monitor.mmGetName(), title) :
                String.format("%0s", title);
    }
    
    /**
     * Simply returns the {@link Object#toString()} of the specified 
     * Object. Should be overridden to enhance output if desired.
     * 
     * @param datum     Object to pretty print
     * @return
     */
    public String prettyPrintDatum(Object datum) {
        return datum.toString();
    }
    
}
