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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

import com.bethecoder.table.AsciiTableInstance;
import com.bethecoder.table.spec.AsciiTable;

/**
 * Contains methods to create the {@link Trace}s used to gather test results
 * and create {@link Metric}s from them for analysis and pretty-printing
 * 
 * This interface contains "defender" methods or Traits that are used to collect
 * result data for the {@link TemporalMemory}.
 * 
 * @author cogmission
 *
 */
public interface TemporalMemoryMonitorMixin extends MonitorMixinBase {
    /**
     * Returns the ComputeDecorator mixin target
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public ComputeDecorator getMonitor();
    
    /**
     * Returns the resetActive flag
     * @return
     */
    public boolean resetActive();
    
    /**
     * Sets the resetActive flag
     * @param b
     */
    public void setResetActive(boolean b);
    
    /**
     * Returns the flag indicating whether the current traces 
     * are stale and need to be recomputed, or not.
     * 
     * @return
     */
    public boolean transitionTracesStale();
    
    /**
     * Sets the flag indicating whether the current traces 
     * are stale and need to be recomputed, or not.
     * 
     * @param b
     */
    public void setTransitionTracesStale(boolean b);
    
    /**
     * Returns Trace of the active {@link Column} indexes.
     * @return
     */
    default IndicesTrace mmGetTraceActiveColumns() {
        return (IndicesTrace)getTraceMap().get("activeColumns");
    }
    
    /**
     * Returns Trace of the active {@link Cell} indexes.
     * @return
     */
    default IndicesTrace mmGetTracePredictiveCells() {
        return (IndicesTrace)getTraceMap().get("predictiveCells");
    }
    
    /**
     * Returns Trace count of {@link Segment}s
     * @return
     */
    default CountsTrace mmGetTraceNumSegments() {
        return (CountsTrace)getTraceMap().get("numSegments");
    }
    
    /**
     * Returns Trace count of {@link Synapse}s
     * @return
     */
    default CountsTrace mmGetTraceNumSynapses() {
        return (CountsTrace)getTraceMap().get("numSynapses");
    }
    
    /**
     * Returns Trace containing a sequence's labels
     * @return
     */
    default StringsTrace mmGetTraceSequenceLabels() {
        return (StringsTrace)getTraceMap().get("sequenceLabels");
    }
    
    /**
     * Returns Trace containing targeted resets for a given sequence
     * @return
     */
    default BoolsTrace mmGetTraceResets() {
        return (BoolsTrace)getTraceMap().get("resets");
    }
    
    /**
     * Trace of predicted => active cells
     * @param c
     * @return
     */
    default IndicesTrace mmGetTracePredictedActiveCells() {
        mmComputeTransitionTraces();
        return (IndicesTrace)getTraceMap().get("predictedActiveCells");
    }
    
    /**
     * Trace of predicted => inactive cells
     * @return
     */
    default IndicesTrace mmGetTracePredictedInactiveCells() {
        mmComputeTransitionTraces();
        return (IndicesTrace)getTraceMap().get("predictedInactiveCells");
    }
    
    /**
     * Trace of predicted => active columns
     * @return
     */
    default IndicesTrace mmGetTracePredictedActiveColumns() {
        mmComputeTransitionTraces();
        return (IndicesTrace)getTraceMap().get("predictedActiveColumns");
    }
    
    /**
     * Trace of predicted => inactive columns
     * @return
     */
    default IndicesTrace mmGetTracePredictedInactiveColumns() {
        mmComputeTransitionTraces();
        return (IndicesTrace)getTraceMap().get("predictedInactiveColumns");
    }
    
    /**
     * Trace of unpredicted => active columns
     * @return
     */
    default IndicesTrace mmGetTraceUnpredictedActiveColumns() {
        mmComputeTransitionTraces();
        return (IndicesTrace)getTraceMap().get("unpredictedActiveColumns");
    }
    
    /**
     * Convenience method to compute a metric over an counts trace, excluding
     * resets.
     * 
     * @param trace     Trace of indices
     * @return
     */
    default Metric mmGetMetricFromTrace(Trace<Number> trace) {
        return Metric.createFromTrace(trace, mmGetTraceResets());
    }
    
    /**
     * Convenience method to compute a metric over an indices trace, excluding
     * resets.
     * 
     * @param trace     Trace of indices
     * @return
     */
    default Metric mmGetMetricFromTrace(IndicesTrace trace) {
        List<LinkedHashSet<Integer>> data = null;
        BoolsTrace excludeResets = mmGetTraceResets();
        if(excludeResets != null) {
            int[] i = { 0 };
            data = trace.items.stream().filter(t -> !excludeResets.items.get(i[0]++)).collect(Collectors.toList());
        }
        
        trace.items = data;
        CountsTrace iTrace = trace.makeCountsTrace();
        return Metric.createFromTrace(iTrace, mmGetTraceResets());
    }
    
    /**
     * Metric for number of predicted => active cells per column for each sequence
     * @return
     */
    @SuppressWarnings("unchecked")
    default Metric mmGetMetricSequencesPredictedActiveCellsPerColumn() {
        mmComputeTransitionTraces();
        
        List<Integer> numCellsPerColumn = new ArrayList<>();
        for(Map.Entry<String, Set<Integer>> m : 
            ((Map<String, Set<Integer>>)getDataMap().get("predictedActiveCellsForSequence")).entrySet()) {
            
            numCellsPerColumn.add(m.getValue().size());
        }
        
        return new Metric(this, "# predicted => active cells per column for each sequence", numCellsPerColumn);
    }
    
    /**
     * Metric for number of sequences each predicted => active cell appears in
     *
     * Note: This metric is flawed when it comes to high-order sequences.
     * @return
     */
    @SuppressWarnings("unchecked")
    default Metric mmGetMetricSequencesPredictedActiveCellsShared() {
        mmComputeTransitionTraces();
        
        Map<Integer, Integer> numSequencesForCell = new HashMap<>();
        
        for(Map.Entry<String, Set<Integer>> m : ((Map<String, Set<Integer>>)getDataMap().get("predictedActiveCellsForSequence")).entrySet()) {
            for(Integer cell : m.getValue()) {
                if(numSequencesForCell.get(cell) == null) {
                    numSequencesForCell.put(cell, 0);
                    continue;
                }
                numSequencesForCell.put(cell, numSequencesForCell.get(cell) + 1);
            }
        }
        
        return new Metric(this, "# sequences each predicted => active cells appears in", new ArrayList<>(numSequencesForCell.values()));
    }
    
    /**
     * Pretty print the connections in the temporal memory.
     * 
     * @return
     */
    default String mmPrettyPrintConnections() {
        StringBuilder text = new StringBuilder();
        
        text.append("Segments: (format => (#) [(source cell=permanence ...),       ...]\n")
        .append("------------------------------------\n");
        
        Connections cnx = getConnections();
        
        List<Integer> columns = Arrays.asList(
            ArrayUtils.toBoxed(
                ArrayUtils.range(0, cnx.getNumColumns())));
        
        for(Integer column : columns) {
            int[] cells = cnx.getColumn(column).getCells().
                stream().map(c -> c.getIndex()).mapToInt(i->i).toArray();
            
            for(int cell : cells) {
                
                Map<Integer, String> segmentDict = new HashMap<>();
                
                for(DistalDendrite dd : cnx.getSegments(cnx.getCell(cell))) {
                    
                    List<Tuple> synapseList = new ArrayList<Tuple>();
                    
                    for(Synapse s : cnx.getSynapses(dd)) {
                        Tuple synapseData = new Tuple(s.getInputIndex(), s.getPermanence());
                        synapseList.add(synapseData);
                    }
                    
                    Stream<Tuple> tupes = synapseList.stream().sorted(
                        (Tuple t1, Tuple t2) -> ((Integer)t1.get(0)).compareTo((Integer)t2.get(0)));
                    
                    List<String> synapseStringList = 
                        tupes.map(t -> String.format("%3d=%.2f", t.get(0), t.get(1))).collect(Collectors.toList());
                    
                    segmentDict.put(dd.getIndex(), 
                        String.format("(%s)", synapseStringList.stream().collect(Collectors.joining(" "))));
                    
                }
                
                text.append(String.format("Column %3d / Cell %3d:\t(%d) %s\n", column, cell, segmentDict.values().size(), 
                    String.format("[%s]", segmentDict.values().stream().collect(Collectors.joining(",       ")))));
            }
            
            if(column < columns.size() - 1) {
                text.append("\n");
            }
        }
        
        text.append("------------------------------------\n");
        
        return text.toString();
    }
    
    /**
     * Pretty print the cell representations for sequences in the history.
     * @return
     */
    @SuppressWarnings("unchecked")
    default String mmPrettyPrintSequenceCellRepresentations(String sortBy) {
        mmComputeTransitionTraces();
        
        String[] header = { "Pattern", "Column", "predicted=>active cells" };
        
        // Check required sort column header to see if it exists, and get index
        int sortIndex = -1;
        int idx = -1;
        for(String colHeader : header) {
            idx++;
            if(colHeader.equals(sortBy)) {
                sortIndex = idx;
                break;
            }
        }
        
        if(sortIndex == -1) {
            throw new IllegalArgumentException("No header named \"" + sortBy + "\" to sort by.");
        }
        
        String[][] data = new String[getDataMap().get("predictedActiveCellsForSequence").values().size()][];
        int i = 0;
        for(Map.Entry<String, Set<Integer>> m : 
            ((Map<String, Set<Integer>>)getDataMap().get("predictedActiveCellsForSequence")).entrySet()) {
            
            Map<Integer, List<Integer>> cellsForColumn = m.getValue().stream().collect(
                Collectors.groupingBy(cell -> getConnections().getCell(cell).getColumn().getIndex()));
            
            for(Integer column : cellsForColumn.keySet()) {
                data[i] = new String[] { 
                    m.getKey(), 
                    column.toString(), 
                    cellsForColumn.get(column).toString().replace("[", "").replace("]", "")
                };
                
                i++;
            }
        }
        
        // Sort the data
        int finalIndex = sortIndex;
        Arrays.stream(data).sorted((sa1, sa2) -> sa1[finalIndex].compareTo(sa2[finalIndex]));
        
        String retVal = AsciiTableInstance.get().getTable(header, data, AsciiTable.ALIGN_CENTER);
        
        return retVal;
    }
    
    // =========================
    // Helper Methods
    // =========================
    
    /**
     * Computes the transition traces, if necessary.
     *
     * Transition traces are the following:
     *
     *  predicted => active cells
     *  predicted => inactive cells
     *  predicted => active columns
     *  predicted => inactive columns
     *  unpredicted => active columns
     */
    @SuppressWarnings("unchecked")
    default void mmComputeTransitionTraces() {
        if(!transitionTracesStale()) {
            return;
        }
        
        Map<String, Set<Integer>> predActCells = null;
        if((predActCells = (Map<String, Set<Integer>>)getDataMap()
            .get("predictedActiveCellsForSequence")) == null) {
            
            getDataMap().put("predictedActiveCellsForSequence", predActCells = new HashMap<String, Set<Integer>>());
        }
        
        getTraceMap().put("predictedActiveCells", new IndicesTrace(this, "predicted => active cells (correct)"));
        getTraceMap().put("predictedInactiveCells", new IndicesTrace(this, "predicted => inactive cells (extra)"));
        getTraceMap().put("predictedActiveColumns", new IndicesTrace(this, "predicted => active columns (correct)"));
        getTraceMap().put("predictedInactiveColumns", new IndicesTrace(this, "predicted => inactive columns (extra)"));
        getTraceMap().put("unpredictedActiveColumns", new IndicesTrace(this, "unpredicted => active columns (bursting)"));
        
        IndicesTrace predictedCellsTrace = (IndicesTrace)getTraceMap().get("predictedCells");
        
        int i = 0;LinkedHashSet<Integer> predictedActiveColumns = null;
        for(Set<Integer> activeColumns : mmGetTraceActiveColumns().items) {
            LinkedHashSet<Integer> predictedActiveCells = new LinkedHashSet<>();
            LinkedHashSet<Integer> predictedInactiveCells = new LinkedHashSet<>();
            predictedActiveColumns = new LinkedHashSet<>();
            LinkedHashSet<Integer> predictedInactiveColumns = new LinkedHashSet<>();
            
            for(Integer predictedCell : predictedCellsTrace.items.get(i)) {
                Integer predictedColumn = getConnections().getCell(predictedCell).getColumn().getIndex();
                
                if(activeColumns.contains(predictedColumn)) {
                    predictedActiveCells.add(predictedCell);
                    predictedActiveColumns.add(predictedColumn);
                    
                    String sequenceLabel = (String)mmGetTraceSequenceLabels().items.get(i);
                    if(sequenceLabel != null && !sequenceLabel.isEmpty()) {
                        Set<Integer> sequencePredictedCells = null;
                        if((sequencePredictedCells = (Set<Integer>)predActCells.get(sequenceLabel)) == null) {
                            
                            ((Map<String, Set<Integer>>)predActCells).put(
                                sequenceLabel, sequencePredictedCells = new LinkedHashSet<Integer>());
                        }
                        
                        sequencePredictedCells.add(predictedCell);
                    }
                }else{
                    predictedInactiveCells.add(predictedCell);
                    predictedInactiveColumns.add(predictedColumn);
                }
            }
            
            LinkedHashSet<Integer> unpredictedActiveColumns = new LinkedHashSet<>(activeColumns);
            unpredictedActiveColumns.removeAll(predictedActiveColumns);
            
            ((IndicesTrace)getTraceMap().get("predictedActiveCells")).items.add(predictedActiveCells);
            ((IndicesTrace)getTraceMap().get("predictedInactiveCells")).items.add(predictedInactiveCells);
            ((IndicesTrace)getTraceMap().get("predictedActiveColumns")).items.add(predictedActiveColumns);
            ((IndicesTrace)getTraceMap().get("predictedInactiveColumns")).items.add(predictedInactiveColumns);
            ((IndicesTrace)getTraceMap().get("unpredictedActiveColumns")).items.add(unpredictedActiveColumns);
            
            i++;
        }
        
        setTransitionTracesStale(false);
    }
    
    // =========================
    // Overrides
    // =========================
    
    default ComputeCycle compute(Connections cnx, int[] activeColumns, String sequenceLabel, boolean learn) {
        // Append last cycle's predictiveCells to *predicTEDCells* trace
        ((IndicesTrace)getTraceMap().get("predictedCells")).items.add(
            new LinkedHashSet<Integer>(Connections.asCellIndexes(cnx.getPredictiveCells())));
        
        ComputeCycle cycle = getMonitor().compute(cnx, activeColumns, learn);
        
        // Append this cycle's predictiveCells to *predicTIVECells* trace
        ((IndicesTrace)getTraceMap().get("predictiveCells")).items.add(
            new LinkedHashSet<Integer>(Connections.asCellIndexes(cnx.getPredictiveCells())));
        
        ((IndicesTrace)getTraceMap().get("activeCells")).items.add(
            new LinkedHashSet<Integer>(Connections.asCellIndexes(cnx.getActiveCells())));
        ((IndicesTrace)getTraceMap().get("activeColumns")).items.add(
            Arrays.stream(activeColumns).boxed().collect(Collectors.toCollection(LinkedHashSet::new)));
        ((CountsTrace)getTraceMap().get("numSegments")).items.add(cnx.numSegments());
        ((CountsTrace)getTraceMap().get("numSynapses")).items.add((int)(cnx.numSynapses() ^ (cnx.numSynapses() >>> 32)));
        ((StringsTrace)getTraceMap().get("sequenceLabels")).items.add(sequenceLabel);
        ((BoolsTrace)getTraceMap().get("resets")).items.add(resetActive());
        
        setResetActive(false);
        
        setTransitionTracesStale(true);
        
        return cycle;
    }

    /**
     * Called to delegate a {@link TemporalMemory#reset(Connections)} call and
     * then set a flag locally which controls remaking of test {@link Trace}s.
     * 
     * @param c
     */
    default void resetSequences(Connections c) {
        getMonitor().reset(c);
        
        setResetActive(true);
    }
    
    /**
     * Returns a list of {@link Trace} objects containing data sets used
     * to analyze the behavior and state of the {@link TemporalMemory} This
     * method is called from all of the "mmXXX" methods to make sure that
     * the data represents the most current execution cycle of the TM.
     * 
     * @param verbosity     setting which controls how much to print out.
     * @return List of {@link Trace}s
     */
    @SuppressWarnings("unchecked")
    default <T extends Trace<?>> List<T> mmGetDefaultTraces(int verbosity) {
        List<T> traces = new ArrayList<>();
        traces.add((T)mmGetTraceActiveColumns());
        traces.add((T)mmGetTracePredictedActiveColumns());
        traces.add((T)mmGetTracePredictedInactiveColumns());
        traces.add((T)mmGetTraceUnpredictedActiveColumns());
        traces.add((T)mmGetTracePredictedActiveCells());
        traces.add((T)mmGetTracePredictedInactiveCells());
        
        List<T> tracesToAdd = new ArrayList<>();
        if(verbosity == 1) {
            for(Trace<?> t : traces) {
                tracesToAdd.add((T)((IndicesTrace)t).makeCountsTrace());
            }
            traces.clear();
            traces.addAll(tracesToAdd);
        }
        
        traces.add((T)mmGetTraceNumSegments());
        traces.add((T)mmGetTraceNumSynapses());
        traces.add((T)mmGetTraceSequenceLabels());
        
        return traces;
    }
    
    /**
     * Returns a list of {@link Metric} objects containing statistics used
     * to analyze the behavior and state of the {@link TemporalMemory} This
     * method is called from all of the "mmXXX" methods to make sure that
     * the data represents the most current execution cycle of the TM.
     * 
     * @param verbosity     setting which controls how much to print out.
     * @return List of {@link Trace}s
     */
    @SuppressWarnings("unchecked")
    default List<Metric> mmGetDefaultMetrics(int verbosity) {
        BoolsTrace resetsTrace = mmGetTraceResets();
        List<Metric> metrics = new ArrayList<>();
        
        List<?> utilTraces = mmGetDefaultTraces(verbosity);
        for(int i = 0;i < utilTraces.size() - 3;i++) {
            metrics.add(Metric.createFromTrace((Trace<Number>)utilTraces.get(i), resetsTrace));
        }
        for(int i = utilTraces.size() - 3;i < utilTraces.size() - 1;i++) {
            metrics.add(Metric.createFromTrace((Trace<Number>)utilTraces.get(i), null));
        }
        metrics.add(mmGetMetricSequencesPredictedActiveCellsPerColumn());
        metrics.add(mmGetMetricSequencesPredictedActiveCellsShared());
        
        return metrics;
    }
    
    /**
     * Clears the map of all {@link Trace}s
     */
    default void mmClearHistory() {
        getTraceMap().clear();
        getDataMap().clear();
        
        getTraceMap().put("predictedCells", new IndicesTrace(this, "predicted cells"));
        getTraceMap().put("activeColumns", new IndicesTrace(this, "active columns"));
        getTraceMap().put("activeCells", new IndicesTrace(this, "active cells"));
        getTraceMap().put("predictiveCells", new IndicesTrace(this, "predictive cells"));
        getTraceMap().put("numSegments", new CountsTrace(this, "# segments"));
        getTraceMap().put("numSynapses", new CountsTrace(this, "# synapses"));
        getTraceMap().put("sequenceLabels", new StringsTrace(this, "sequence labels"));
        getTraceMap().put("resets", new BoolsTrace(this, "resets"));
        
        setTransitionTracesStale(true);
    }
}
