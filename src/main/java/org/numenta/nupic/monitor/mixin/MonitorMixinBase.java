package org.numenta.nupic.monitor.mixin;

import java.util.List;
import java.util.Map;

import org.numenta.nupic.Connections;

import com.sun.javafx.font.Metrics;

/**
 * Base class for MonitorMixin. Each subclass will be a mixin for a particular
 * algorithm.
 *
 * All arguments, variables, and methods in monitor mixin classes should be
 * prefixed with "mm" (to avoid collision with the classes they mix in to).
 * 
 * To make a "Mixin":
 * <OL>
 *      <li>Create an interface (A) containing overridden methods</li>
 *      <li>Create MonitorMixinBase extension mixin type (this does most of the mixin work)</li>
 *      <li>Create constructor in extension class which takes above interface (A) which is the target class which must implement (A)</li>
 *      
 * 
 * @author David Ray
 */
public interface MonitorMixinBase {
    public <T> T getMonitor();
    
    public Connections getConnections();
    
    public Map<String, Trace<?>> getTraceMap();
    
    public Map<String, Map<String, ?>> getDataMap();
    
    public String mmGetName();
    
    public void mmClearHistory();
    
    public <T extends Trace<?>> List<T> mmGetDefaultTraces();
    
    public <T extends Trace<?>> List<T> mmGetDefaultMetrics();
    
    public static String mmPrettyPrintTraces(List<Trace<?>> traces, int sigFigs) {
        return null;
    }
    
    public static String mmPrettyPrintMetrics(Metrics metrics, int sigFigs) {
        return null;
    }
}
