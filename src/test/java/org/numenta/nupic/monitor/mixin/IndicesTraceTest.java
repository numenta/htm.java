package org.numenta.nupic.monitor.mixin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;


public class IndicesTraceTest {
    private IndicesTrace trace;
    
    @Before
    public void setUp() {
        trace = new IndicesTrace(new MonitorMixinBase() {
            @Override
            public Map<String, Trace<?>> getTraces() {
                // TODO Auto-generated method stub
                return null;
            }}, "active cells");
        trace.data.add(new LinkedHashSet<Integer>(Arrays.asList(1, 2, 3)));
        trace.data.add(new LinkedHashSet<Integer>(Arrays.asList(4, 5)));
        trace.data.add(new LinkedHashSet<Integer>(Arrays.asList(6)));
        trace.data.add(new LinkedHashSet<Integer>());
    }
    
    @Test
    public void testMakeCountsTrace() {
        CountsTrace countsTrace = trace.makeCountsTrace();
        assertEquals("# active cells", countsTrace.title);
        assertTrue(countsTrace.data.equals(Arrays.asList(3, 2, 1, 0)));
    }
    
    @Test
    public void testMakeCumCountsTrace() {
        CountsTrace countsTrace = trace.makeCumCountsTrace();
        assertEquals("# (cumulative) active cells", countsTrace.title);
        assertTrue(countsTrace.data.equals(Arrays.asList(3, 5, 6, 6)));
    }
}
