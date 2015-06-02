/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.datagen.PatternMachine;
import org.numenta.nupic.datagen.SequenceMachine;
import org.numenta.nupic.integration.TemporalMemoryTestMachine.DetailedResults;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Base class for integration tests of the {@link TemporalMemory}
 * 
 * @author Chetan Surpur
 * @author David Ray
 * @see BasicTemporalMemoryTest
 */
public abstract class AbstractTemporalMemoryTest {
    protected TemporalMemory tm;
    protected Connections connections;
    protected PatternMachine patternMachine;
    protected SequenceMachine sequenceMachine;
    protected TemporalMemoryTestMachine tmTestMachine;
    protected Parameters parameters;
    protected List<Set<Integer>> sequence;
    
    /**
     * Called from each test to instantiate a fresh {@link TemporalMemory}
     * object with configured parameters for the test.
     * 
     * @see Parameters
     * @see TemporalMemory
     */
    protected void initTM() {
        tm = new TemporalMemory();
        connections = new Connections();
        if(parameters != null) {
            parameters.apply(connections);
        }
        tm.init(connections);
    }
    
    /**
     * Validates the {@link Parameters} and their existence.
     * @return
     */
    private boolean checkParams() {
        return parameters != null;
    }
    
    /**
     * Initializes the test data generators
     * 
     * @param patternMachine
     */
    protected void finishSetUp(PatternMachine patternMachine) {
        this.patternMachine = patternMachine;
        this.sequenceMachine = new SequenceMachine(patternMachine);
        this.tmTestMachine = new TemporalMemoryTestMachine(tm, connections);
    }
    
    /**
     * Displays the test setup
     * 
     * @param sequence      list of sequences to be input the {@link TemporalMemory}
     * @param learn         flag indicating whether the algorithm will execute learning functions
     * @param num           number of times "sequence" should be repeated
     */
    protected void showInput(List<Set<Integer>> sequence, boolean learn, int num) {
        if(checkParams()) {
            System.out.println("New TemporalMemory Parameters:");
            System.out.println(parameters);
        }
        
        String sequenceText = sequenceMachine.prettyPrintSequence(sequence, 1);
        
        String learnText = learn ? "(learning enabled)" : "(learning disabled)";
        System.out.println("Feeding sequence " + learnText + " " + sequenceText + " [" + num + " times]");
    }
    
    /**
     * Starts the inputting of sequence(s) into the {@link TemporalMemory}
     * 
     * @param sequence      list of sequences to be input the {@link TemporalMemory}
     * @param learn         flag indicating whether the algorithm will execute learning functions
     * @param num           number of times "sequence" should be repeated
     * @return
     */
    protected DetailedResults feedTM(List<Set<Integer>> sequence, boolean learn, int num) {
        showInput(sequence, learn, num);
        
        List<Set<Integer>> actual = new ArrayList<Set<Integer>>(sequence);
        if(num > 1) {
            for(int i = 1;i < num;i++) {
                actual.addAll(sequence);
            }
        }
        
        List<Set<Integer>> results = tmTestMachine.feedSequence(actual, learn);
        
        DetailedResults detailedResults = tmTestMachine.computeDetailedResults(results, actual);
        
        String ppResults = tmTestMachine.prettyPrintDetailedResults(detailedResults, actual, patternMachine, 1);
        
        System.out.println(ppResults);
        
        System.out.println("");
        
        if(learn) {
            System.out.println(tmTestMachine.prettyPrintTemporalMemory());
        }
        
        return detailedResults;
    }
    
    

    

}
