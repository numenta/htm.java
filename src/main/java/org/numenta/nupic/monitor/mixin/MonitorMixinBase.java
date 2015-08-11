package org.numenta.nupic.monitor.mixin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Base class for MonitorMixin. Each subclass will be a mixin for a particular
 * algorithm.
 *
 * All arguments, variables, and methods in monitor mixin classes should be
 * prefixed with "mm" (to avoid collision with the classes they mix in to).
 * 
 * To make a "Mixin":
 * <OL>
 *      <li>Create an interface containing overridden methods</li>
 *      <li>Create MonitorMixinBase extension</li>
 *      <li>Create constructor in extension class which takes above interface & target class</li>
 *      
 * 
 * @author David Ray
 */
public interface MonitorMixinBase {
    Map<String, Trace> getTraces();
    
    default String mmGetName() {
        return null;
    }
    
    default <T extends Trace> List<T> mmGetDefaultTraces() {
        return Collections.emptyList();
    }
}
