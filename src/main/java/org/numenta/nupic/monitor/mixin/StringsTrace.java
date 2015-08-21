package org.numenta.nupic.monitor.mixin;


/**
 * Each entry contains strings (for example sequence labels).
 *  
 * @author cogmission
 */
public class StringsTrace extends Trace<String> {

    public StringsTrace(MonitorMixinBase monitor, String title) {
        super(monitor, title);
    }
}
