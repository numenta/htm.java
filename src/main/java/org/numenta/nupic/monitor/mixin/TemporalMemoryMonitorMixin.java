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

import org.numenta.nupic.ComputeCycle;
import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.monitor.ComputeDecorator;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;


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
     * Convenience method to compute a metric over an indices trace, excluding
     * resets.
     * 
     * @param trace     Trace of indices
     * @return
     */
    default Metric mmGetMetricFromTrace(Trace<Number> trace) {
        return Metric.createFromTrace(trace, mmGetTraceResets());
    }
    
    /**
     * Metric for number of predicted => active cells per column for each sequence
     * @return
     */
    @SuppressWarnings("unchecked")
    default Metric mmGetMetricSequencesPredictedActiveCellsPerColumn() {
        mmComputeTransitionTraces();
        
        List<Integer> numCellsPerColumn = new ArrayList<>();
        for(Map.Entry<String, Set<Integer>> m : ((Map<String, Set<Integer>>)getDataMap().get("predictedActiveCellsForSequence").values()).entrySet()) {
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
        
        for(Map.Entry<String, Set<Integer>> m : ((Map<String, Set<Integer>>)getDataMap().get("predictedActiveCellsForSequence").values()).entrySet()) {
            for(Integer cell : m.getValue()) {
                numSequencesForCell.put(cell, numSequencesForCell.get(cell) + 1);
            }
        }
        
        return new Metric(this, "# sequences each predicted => active cells appears in", new ArrayList<>(numSequencesForCell.values()));
    }
    
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
                        tupes.map(t -> String.format("%3d=.2f", t.get(0), t.get(1))).collect(Collectors.toList());
                    
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
        
        int i = 0;
        for(Set<Integer> activeColumns : mmGetTraceActiveColumns().data) {
            LinkedHashSet<Integer> predictedActiveCells = new LinkedHashSet<>();
            LinkedHashSet<Integer> predictedInactiveCells = new LinkedHashSet<>();
            LinkedHashSet<Integer> predictedActiveColumns = new LinkedHashSet<>();
            LinkedHashSet<Integer> predictedInactiveColumns = new LinkedHashSet<>();
            
            for(Integer predictedCell : predictedCellsTrace.data.get(i)) {
                Integer predictedColumn = getConnections().getCell(predictedCell).getColumn().getIndex();
                
                if(activeColumns.contains(predictedColumn)) {
                    predictedActiveCells.add(predictedCell);
                    predictedActiveColumns.add(predictedColumn);
                    
                    String sequenceLabel = (String)mmGetTraceSequenceLabels().data.get(i);
                    if(sequenceLabel != null) {
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
            
            LinkedHashSet<Integer> unpredictedActiveColumns = 
                activeColumns.stream().filter(c -> !predictedActiveColumns.contains(i)).collect(Collectors.toCollection(LinkedHashSet::new));
            
            ((IndicesTrace)getTraceMap().get("predictedActiveCells")).data.add(predictedActiveCells);
            ((IndicesTrace)getTraceMap().get("predictedInactiveCells")).data.add(predictedInactiveCells);
            ((IndicesTrace)getTraceMap().get("predictedActiveColumns")).data.add(predictedActiveColumns);
            ((IndicesTrace)getTraceMap().get("predictedInactiveColumns")).data.add(predictedInactiveColumns);
            ((IndicesTrace)getTraceMap().get("unpredictedActiveColumns")).data.add(unpredictedActiveColumns);
            
            setTransitionTracesStale(false);
        }
    }
    
    default ComputeCycle compute(Connections connections, int[] activeColumns, String sequenceLabel, boolean learn) {
        
        return getMonitor().compute(connections, activeColumns, learn);
    }
}
