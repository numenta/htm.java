package org.numenta.nupic.monitor.mixin;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.numenta.nupic.util.ArrayUtils;


/**
 * {@code Metric} class used in monitor mixin framework.
 * 
 * A metric computed over a set of data (usually from a {@link CountsTrace}).
 * 
 * @author Chetan Surpur
 * @author cogmission
 */
public class Metric {
    public MonitorMixinBase monitor;
    public String title;
    
    public double min;
    public double max;
    public double sum;
    public double mean = Double.NaN;
    public double variance;
    public double standardDeviation;
    
    public Metric(MonitorMixinBase monitor, String title, List<? extends Number> l) {
        this.monitor = monitor;
        this.title = title;
        
        computeStats(l);
    }
    
    public static <T extends Trace<? extends Number>> Metric createFromTrace(T trace, BoolsTrace excludeResets) {
        List<? extends Number> data = trace.items;
        if(excludeResets != null) {
            int[] i = { 0 };
            data = trace.items.stream().filter(t -> !excludeResets.items.get(i[0]++)).collect(Collectors.toList());
        }
        return new Metric(trace.monitor, trace.title, data);
    }
    
    public Metric copy() {
        Metric metric = new Metric(monitor, title, Collections.emptyList());
        
        metric.min = min;
        metric.max = max;
        metric.sum = sum;
        metric.mean = mean;
        metric.variance = variance;
        metric.standardDeviation = standardDeviation;
        
        return metric;
    }
    
    public String prettyPrintTitle() {
        return String.format(monitor.mmGetName() == null ? "%s" : "[%s] %s", 
            monitor.mmGetName() == null ? new String[] { title } : new String[] { monitor.mmGetName(), title});
    }
    
    public void computeStats(List<? extends Number> l) {
        if(l.size() < 1) {
            return;
        }
        
        double[] doubs = null;
        if(Integer.class.isAssignableFrom(l.get(0).getClass())) {
            doubs = ArrayUtils.toDoubleArray(ArrayUtils.toPrimitive(l.toArray(new Integer[l.size()])));
        }else if(Double.class.isAssignableFrom(l.get(0).getClass())) {
            doubs = ArrayUtils.toPrimitive(l.toArray(new Double[l.size()]));
        }
        
        min = ArrayUtils.min(doubs);
        max = ArrayUtils.max(doubs);
        sum = ArrayUtils.sum(doubs);
        
        double d = ArrayUtils.average(doubs);
        mean = d;
        double v = ArrayUtils.variance(doubs, d);
        variance = v;
        double s = v > 0 ? Math.sqrt(v) : 0.0;
        standardDeviation = s;
    }
    
    public double[] getStats(int sigFigs) {
        if(Double.isNaN(mean)) {
            return new double[] { 0, 0, 0, 0, 0 };
        }
        return new double[] {
            BigDecimal.valueOf(mean).setScale(sigFigs, BigDecimal.ROUND_HALF_UP).doubleValue(),
            BigDecimal.valueOf(standardDeviation).setScale(sigFigs, BigDecimal.ROUND_HALF_UP).doubleValue(),
            BigDecimal.valueOf(min).setScale(sigFigs, BigDecimal.ROUND_HALF_UP).doubleValue(),
            BigDecimal.valueOf(max).setScale(sigFigs, BigDecimal.ROUND_HALF_UP).doubleValue(),
            BigDecimal.valueOf(sum).setScale(sigFigs, BigDecimal.ROUND_HALF_UP).doubleValue()
        };
    }
}
