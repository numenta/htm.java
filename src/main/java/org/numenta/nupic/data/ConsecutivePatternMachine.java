package org.numenta.nupic.data;

import java.util.LinkedHashSet;

/**
 * Utilities for generating and manipulating patterns consisting of consecutive
 * sequences of numbers, for use in experimentation and tests.
 * 
 * @author David Ray
 *
 * @see PatternMachine
 */
public class ConsecutivePatternMachine extends PatternMachine {
	public ConsecutivePatternMachine(int n, int w) {
		super(n, w);
	}
	
	@Override
	public void generate() {
		LinkedHashSet<Integer> pattern;
		for(int i = 0;i < n / w;i++) {
			pattern = xrange(i * w, (i + 1) * w);
			patterns.put(i, pattern);
		}
	}
}