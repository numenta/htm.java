package org.numenta.nupic.monitor.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Each entry contains indices (for example of predicted => active cells).
 * 
 * @author cogmission
 */
public class IndicesTrace extends Trace<Set<Integer>> {

    /**
     * Constructs a new {@code IndicesTrace}
     * 
     * @param monitor
     * @param title
     */
    public IndicesTrace(MonitorMixinBase monitor, String title) {
        super(monitor, title);
    }
    
    /**
     * A new Trace made up of counts of this trace's indices.
     * @return
     */
    public CountsTrace makeCountsTrace() {
        CountsTrace trace = new CountsTrace(monitor, String.format("# %s", title));
        trace.data = data.stream().map(l -> l.size()).collect(Collectors.toList());
        return trace;
    }

    public CountsTrace makeCumCountsTrace() {
        CountsTrace trace = new CountsTrace(monitor, String.format("# (cumulative) %s", title));
        Trace<Integer> countsTrace = makeCountsTrace();
        
        int[] accum = { 0 };
        trace.data = countsTrace.data.stream().map(i -> accum[0] += ((int)i)).collect(Collectors.toList());
        
        return trace;
    }
    
    /**
     * Prints the specified datum
     * @param c
     * @return
     */
    public String prettyPrintDatum(Collection<Integer> c) {
        Collections.sort(new ArrayList<>(c));
        return c.toString();
    }
}
