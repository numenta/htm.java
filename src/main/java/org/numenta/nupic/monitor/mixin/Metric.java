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
package org.numenta.nupic.monitor.mixin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    
    /**
     * Returns a {@code Metric} object created from the specified {@link Trace}
     * @param trace
     * @param excludeResets
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends Trace<? extends Number>> Metric createFromTrace(T trace, BoolsTrace excludeResets) {
        List<Number> data = (List<Number>)trace.items;
        if(excludeResets != null) {
            data = new ArrayList<>();
            for(int k = 0;k < trace.items.size();k++) {
                if(!excludeResets.items.get(k)) {
                    Number n = trace.items.get(k);
                    data.add(n);
                }
            }
        }
        return new Metric(trace.monitor, trace.title, data);
    }
    
    /**
     * Returns a copy of this {@code Metric}
     * @return
     */
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
    
    /**
     * Populates the inner fields of this {@code Metric} with the computed stats.
     * @param l
     */
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
    
    /**
     * Returns an array of this {@link Metric}'s stats.
     * @param sigFigs   the number of significant figures to limit the output numbers to.
     * @return
     */
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
