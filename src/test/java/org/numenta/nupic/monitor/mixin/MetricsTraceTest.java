package org.numenta.nupic.monitor.mixin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.monitor.MonitoredTemporalMemory;


public class MetricsTraceTest {

    @Test
    public void testPrettyPrintDatum() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        Connections connections = new Connections();
        parameters.apply(connections);
        
        TemporalMemory temporalMemory = new TemporalMemory();
        TemporalMemory.init(connections);
        MonitoredTemporalMemory monitoredTM = new MonitoredTemporalMemory(temporalMemory, connections);
        
        Metric metric = new Metric(monitoredTM, "Test", Arrays.asList(2.3, 3.4, 5.5, 6.6, 7.7));
        
        MetricsTrace trace = null;
        String traceData = null;
        try {
            trace = new MetricsTrace(monitoredTM, "Test");
            traceData = trace.prettyPrintDatum(metric);
        }catch(Exception e) {
            fail();
        }
                        
        assertEquals("min: 2.30, max: 7.70, sum: 25.50, mean: 5.10, std dev: 1.99", traceData);
    }

}
