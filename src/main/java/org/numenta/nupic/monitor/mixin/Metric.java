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
    MonitorMixinBase monitor;
    String title;
    
    double min;
    double max;
    double sum;
    double mean = Double.NaN;
    double variance;
    double standardDeviation;
    
    public Metric(MonitorMixinBase monitor, String title, List<? extends Number> l) {
        this.monitor = monitor;
        this.title = title;
        
        computeStats(l);
    }
    
    public static <T extends Trace<? extends Number>> Metric createFromTrace(T trace, BoolsTrace excludeResets) {
        List<? extends Number> data = trace.data;
        if(excludeResets != null) {
            int[] i = { 0 };
            data = trace.data.stream().filter(t -> !excludeResets.data.get(i[0]++)).collect(Collectors.toList());
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
        return String.format("[%s] %s", monitor.mmGetName(), title);
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
