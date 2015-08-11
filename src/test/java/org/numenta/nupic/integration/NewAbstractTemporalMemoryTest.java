package org.numenta.nupic.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.PatternMachine;
import org.numenta.nupic.datagen.SequenceMachine;
import org.numenta.nupic.monitor.mixin.MonitorMixinBase;
import org.numenta.nupic.research.NewTemporalMemory;


public class NewAbstractTemporalMemoryTest {
    protected NewTemporalMemory tm;
    protected Parameters parameters;
    protected Connections connections;
    protected PatternMachine patternMachine;
    protected SequenceMachine sequenceMachine;
    
    public void init(Parameters overrides, PatternMachine pm) {
        this.parameters = createTMParams(overrides);
        this.connections = new Connections();
        parameters.apply(connections);
        
        tm = new NewTemporalMemory();
        tm.init(connections);
        
        this.patternMachine = pm;
        this.sequenceMachine = new SequenceMachine(patternMachine);
    }
    
    /**
     * Creates {@link Parameters} for tests
     */
    protected Parameters createTMParams(Parameters overrides) {
        parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 100 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 1);
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.7);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 11);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 11);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.4);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.0);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 11);
        
        if(overrides != null) {
            parameters.union(overrides);
        }
        
        return parameters;
    }
    
    public void feedTM(List<Set<Integer>> sequence, boolean learn, int num) {
        List<Set<Integer>> repeatedSequence = new ArrayList<Set<Integer>>(sequence);
        if(num > 1) {
            for(int i = 1;i < num;i++) {
                repeatedSequence.addAll(sequence);
            }
        }
        
        //tm.mmClearHistory();
        
        for(Set<Integer> pattern : repeatedSequence) {
            
        }
    }
    
    public static void main(String[] args) {
        class A implements MonitorMixinBase {};
        
        class B implements MonitorMixinBase {};
        
        A a = new A(); a.setName("A");
        B b = new B(); b.setName("B");
        
        System.out.println("A name = " + a.getName());
        System.out.println("B name = " + b.getName());
    }
}
