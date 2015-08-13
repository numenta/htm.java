package org.numenta.nupic.monitor.mixin;

/**
 * Each entry contains {@link Metrics} (for example metric for # of predicted => active
 * cells).
 * 
 * @author cogmission
 */
public class MetricsTrace extends Trace<Double> {

    public MetricsTrace(MonitorMixinBase monitor, String title) {
        super(monitor, title);
    }

    public String prettyPrintDatum(Metric datum) {
        return String.format("min: %.2f, max: %.2f, sum: %.2f, mean: %.2f, std dev: %.2f", 
            datum.min, datum.max, datum.sum, datum.mean, datum.standardDeviation);
    }
}
