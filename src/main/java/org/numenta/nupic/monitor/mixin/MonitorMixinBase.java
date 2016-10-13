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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.model.Connections;
import org.numenta.nupic.monitor.MonitoredTemporalMemory;

import com.bethecoder.table.AsciiTableInstance;
import com.bethecoder.table.spec.AsciiTable;


/**
 * Base class for MonitorMixin. Each subclass will be a mixin for a particular
 * algorithm.
 *
 * All arguments, variables, and methods in monitor mixin classes should be
 * prefixed with "mm" (to avoid collision with the classes they mix in to).
 * 
 * To make a "Mixin":
 * <OL>
 *      <li>Create a Decorator interface (A) containing overridden methods</li>
 *      <li>Create MonitorMixinBase extension mixin type (interface) (this does most of the mixin work) see {@link TemporalMemoryMonitorMixin}</li>
 *      <li>Create Extension of both {@link MonitorMixinBase} and interface (see {@link MonitoredTemporalMemory})</li>
 *      <li>Create constructor in extension class which takes above interface (A) which is the target class which must implement (A)</li>
 *      <li>Make the Original class (see {@link TemporalMemory}) to be tested, implement the Decorator interface (A)</li>
 *      <li>Use the "joining" extension class as you would the original class</li>
 * </OL>
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
    
    public <T extends Trace<?>> List<T> mmGetDefaultTraces(int verbosity);
    
    public List<Metric> mmGetDefaultMetrics(int verbosity);
    
    default String mmPrettyPrintTraces(List<Trace<?>> traces, BoolsTrace breakOnResets) {
        String[] header = new String[traces.size() + 1];
        header[0] = "#";
        
        for(int i = 0;i < traces.size();i++) header[i + 1] = traces.get(i).prettyPrintTitle();
        
        int len = traces.get(0).items.size();
        List<String[]> table = new ArrayList<>(len);
        for(int i = 0;i < len;i++) {
            if(breakOnResets != null && breakOnResets.items.get(i)) {
                table.add(Collections.nCopies(header.length + 1, "<reset>").toArray(new String[header.length + 1]));
            }
            String[] sa = new String[traces.size() + 1];
            sa[0] = "" + i;
            int x = 1;
            for(Trace<?> t : traces) {
                sa[x++] = t.prettyPrintDatum(t.items.get(i)); 
            }
            table.add(sa);
        }
        
        String retVal = AsciiTableInstance.get().getTable(header, table.toArray(new String[table.size()][]), AsciiTable.ALIGN_CENTER);
        return retVal;
    }
    
    default String mmPrettyPrintMetrics(List<Metric> metrics, int sigFigs) {
        String hashes = "";
        for(int i = 0;i < Math.max(2, sigFigs);i++) {
            hashes += "#";
        }
        
        DecimalFormat df = new DecimalFormat("0." + hashes);
        
        String[] header = new String[] { "Metric", "mean", "standard deviation", "min", "max", "sum" };
        
        String[][] data = new String[metrics.size()][header.length];
        int i = 0;
        for(Metric metric : metrics) {
            for(int j = 0;j < header.length;j++) {
                if(j == 0) {
                    data[i][j] = metric.prettyPrintTitle();
                }else{
                    double[] stats = metric.getStats(sigFigs);
                    data[i][j] = ((int)stats[j - 1]) == stats[j - 1] ? 
                        df.format(stats[j - 1]) + ".0" : df.format(stats[j - 1]);
                }
            }
            i++;
        }
        
        String retVal = AsciiTableInstance.get().getTable(header, data, AsciiTable.ALIGN_CENTER);
        return retVal;
    }
}
