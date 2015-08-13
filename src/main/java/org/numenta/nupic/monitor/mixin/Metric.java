package org.numenta.nupic.monitor.mixin;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * {@code Metric} class used in monitor mixin framework.
 * 
 * A metric computed over a set of data (usually from a {@link CountsTrace}).
 * 
 * @author Chetan Surpur
 * @author cogmission
 */
public class Metric {
    MonitorMixinBase monitor;
    String title;
    List<Double> data;
    
    double min, max, sum, mean, standardDeviation;
    
    public Metric(MonitorMixinBase monitor, String title, List<Double> l) {
        this.monitor = monitor;
        this.title = title;
        this.data = l;
    }
    
    public static Metric createFromTrace(Trace<Double> trace, BoolsTrace excludeResets) {
        List<Double> data = trace.data;
        if(excludeResets != null) {
            int[] i = { 0 };
            data = trace.data.stream().filter(t -> excludeResets.data.get(i[0]++)).collect(Collectors.toList());
        }
        
        return new Metric(trace.monitor, trace.title, data);
    }
    
    public Metric copy() {
        Metric metric = new Metric(monitor, title, Collections.emptyList());
    }
}
