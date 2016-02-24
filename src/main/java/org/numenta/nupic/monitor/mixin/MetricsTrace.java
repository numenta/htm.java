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
import java.util.Locale;
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
        return String.format(Locale.US, "min: %.2f, max: %.2f, sum: %.2f, mean: %.2f, std dev: %.2f", 
            datum.min, datum.max, datum.sum, datum.mean, datum.standardDeviation);
    }
}
