package org.numenta.nupic.monitor.mixin;

import java.util.stream.Collectors;

/**
 * Each entry contains indices (for example of predicted => active cells).
 * 
 * @author cogmission
 */
public class IndicesTrace extends Trace {

    /**
     * Constructs a new {@code IndicesTrace}
     * 
     * @param monitor
     * @param title
     */
    public IndicesTrace(MonitorMixinBase monitor, String title) {
        super(monitor, title);
    }
    
    public Trace makeCountsTrace() {
        Trace trace = new CountsTrace(monitor, String.format("# %s", title));
        trace.data = data.stream().collect(Collectors.toList()); 
    }

}
