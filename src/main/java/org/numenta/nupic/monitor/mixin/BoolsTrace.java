package org.numenta.nupic.monitor.mixin;

/**
 * Each entry contains booleans (for example resets).
 * 
 * @author cogmission
 */
public class BoolsTrace extends Trace<Boolean> {

    public BoolsTrace(MonitorMixinBase monitor, String title) {
        super(monitor, title);
    }

}
