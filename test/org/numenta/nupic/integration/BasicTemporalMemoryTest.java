package org.numenta.nupic.integration;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.data.ConsecutivePatternMachine;
import org.numenta.nupic.integration.TemporalMemoryTestMachine.DetailedResults;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;

public class BasicTemporalMemoryTest extends AbstractTemporalMemoryTest {
	
	/**
	 * Basic static input for all tests in this class
	 */
	private void defaultSetup() {
		parameters = new Parameters();
		EnumMap<Parameters.KEY, Number> p = parameters.getMap();
		p.put(KEY.COLUMN_DIMENSIONS, 6);
		p.put(KEY.CELLS_PER_COLUMN, 4);
		p.put(KEY.INITIAL_PERMANENCE, 0.3);
		p.put(KEY.CONNECTED_PERMANENCE, 0.5);
		p.put(KEY.MIN_THRESHOLD, 1);
		p.put(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
		p.put(KEY.PERMANENCE_INCREMENT, 0.1);
		p.put(KEY.PERMANENCE_DECREMENT, 0.05);
		p.put(KEY.ACTIVATION_THRESHOLD, 1);
	}
	
	/**
	 * Basic first order sequences
	 */
	@Test
	public void testA() {
		defaultSetup();
		
		//Basic first order sequences
		initTM();
		
		assertEquals(0.05, tm.getPermanenceDecrement(), .001);
		assertEquals(0.1, tm.getPermanenceIncrement(), .001);
		
		finishSetUp(new ConsecutivePatternMachine(6, 1));
		
		List<Integer> input = Arrays.asList(new Integer[] { 0, 1, 2, 3, -1 });
		sequence = sequenceMachine.generateFromNumbers(input);
		
		DetailedResults detailedResults = feedTM(sequence, true, 1);
		assertEquals(0, detailedResults.predictedActiveColumnsList.get(3).size(), 0);
		
		feedTM(sequence, true, 2);
		
		detailedResults = feedTM(sequence, true, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
		
		feedTM(sequence, true, 4);
		
		detailedResults = feedTM(sequence, true, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
	}
	
	/**
	 * High order sequences in order
	 */
	@Test
	public void testB() {
		defaultSetup();
		
		//Basic first order sequences
		initTM();
		
		assertEquals(0.05, tm.getPermanenceDecrement(), .001);
		assertEquals(0.1, tm.getPermanenceIncrement(), .001);
		
		finishSetUp(new ConsecutivePatternMachine(6, 1));
		
		List<Integer> inputA = Arrays.asList(new Integer[] { 0, 1, 2, 3, -1 });
		List<Integer> inputB = Arrays.asList(new Integer[] { 4, 1, 2, 5, -1 });
		
		List<Set<Integer>> sequenceA = sequenceMachine.generateFromNumbers(inputA);
		List<Set<Integer>> sequenceB = sequenceMachine.generateFromNumbers(inputB);
		
		feedTM(sequenceA, true, 5);
		
		DetailedResults detailedResults = feedTM(sequenceA, false, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
		
		feedTM(sequenceB, true, 1);
		feedTM(sequenceB, true, 2);
		detailedResults = feedTM(sequenceB, false, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(1).size());
		
		feedTM(sequenceB, true, 3);
		detailedResults = feedTM(sequenceB, false, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(2).size());
		
		feedTM(sequenceB, true, 3);
		detailedResults = feedTM(sequenceB, false, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
		
		detailedResults = feedTM(sequenceA, false, 1);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
		assertEquals(1, detailedResults.predictedInactiveColumnsList.get(3).size());
		
		feedTM(sequenceA, true, 10);
		assertEquals(1, detailedResults.predictedActiveColumnsList.get(3).size());
		/** TODO: 	Requires some form of synaptic decay to forget the ABC=>Y
		 * 			transition that's initially formed
		 * assertEquals(1, detailedResults.predictedInactiveColumnsList.get(3).size());
		 */
	}

}
