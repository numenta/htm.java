package org.numenta.nupic.monitor.mixin;


/**
 * A new Trace made up of counts of this trace's indices.
 * 
 * @author cogmission
 */
public class CountsTrace extends Trace<Integer> {

    public CountsTrace(MonitorMixinBase monitor, String title) {
        super(monitor, title);
    }
}
