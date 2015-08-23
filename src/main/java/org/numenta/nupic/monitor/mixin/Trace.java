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

import java.util.ArrayList;
import java.util.List;


/**
 * A record of the past data the algorithm has seen, with an entry for each
 * iteration.
 * 
 * Contains {@code Trace} classes used in monitor mixin framework.
 * 
 * @author Chetan Surpur
 * @author cogmission
 */
public abstract class Trace<T> {
    protected MonitorMixinBase monitor;
    protected String title;
    
    public List<T> items;
    
    /**
     * Constructs a new {@code Trace}
     * @param monitor
     * @param title
     */
    public Trace(MonitorMixinBase monitor, String title) {
        this.monitor = monitor;
        this.title = title;
        
        items = new ArrayList<T>();
    }
    
    /**
     * Returns the implementing mixin name if not null 
     * plus the configured title.
     * 
     * @return
     */
    public String prettyPrintTitle() {
        return monitor.mmGetName() != null ? 
            String.format("[%s] %1s", monitor.mmGetName(), title) :
                String.format("%s", title);
    }
    
    /**
     * Simply returns the {@link Object#toString()} of the specified 
     * Object. Should be overridden to enhance output if desired.
     * 
     * @param datum     Object to pretty print
     * @return
     */
    public String prettyPrintDatum(Object datum) {
        return datum.toString();
    }
    
}
