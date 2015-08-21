package org.numenta.nupic.monitor.mixin;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;


public class MetricTest {
    private CountsTrace countsTrace;
    
    @Before
    public void test() {
        countsTrace = new CountsTrace(null, "# active cells");
        countsTrace.items = Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 0 });
    }
    
    @Test
    public void testCreateFromTrace() {
        Metric metric = Metric.createFromTrace(countsTrace, null);
        assertEquals(metric.title, countsTrace.title);
        assertEquals(0, metric.min, 0.1);
        assertEquals(5, metric.max, 0);
        assertEquals(15, metric.sum, 0);
        assertEquals(2.5, metric.mean, 0.01);
        assertEquals(1.707825127659933, metric.standardDeviation, 0.00000000001);
    }
    
    @Test
    public void testCreateFromTraceExcludeResets() {
        BoolsTrace resetTrace = new BoolsTrace(null, "resets");
        resetTrace.items = Arrays.asList(new Boolean[] { true, false, false, true, false, false });
        
        Metric metric = Metric.createFromTrace(countsTrace, resetTrace);
        assertEquals(metric.title, countsTrace.title);
        assertEquals(0, metric.min, 0.1);
        assertEquals(5, metric.max, 0);
        assertEquals(10, metric.sum, 0);
        assertEquals(2.5, metric.mean, 0.01);
        assertEquals(1.8027756377319946, metric.standardDeviation, 0.00000000001);
    }

}
