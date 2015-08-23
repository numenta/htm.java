package org.numenta.nupic.monitor.mixin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Each entry contains indices (for example of predicted => active cells).
 * 
 * @author cogmission
 */
public class IndicesTrace extends Trace<LinkedHashSet<Integer>> {
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
        trace.items = items.stream().map(l -> l.size()).collect(Collectors.toList());
        return trace;
    }

    /**
     * Trace made up of cumulative counts of trace indices.
     * @return
     */
    public CountsTrace makeCumCountsTrace() {
        CountsTrace trace = new CountsTrace(monitor, String.format("# (cumulative) %s", title));
        Trace<Integer> countsTrace = makeCountsTrace();
        
        int[] accum = { 0 };
        trace.items = countsTrace.items.stream().map(i -> accum[0] += ((int)i)).collect(Collectors.toList());
        
        return trace;
    }
    
    /**
     * Prints the specified datum
     * @param c
     * @return
     */
    public String prettyPrintDatum(Collection<Integer> c) {
        List<Integer> l = null;
        Collections.sort(l = new ArrayList<>(c));
        return l.toString().replace("[", "").replace("]", "").trim();
    }
}
