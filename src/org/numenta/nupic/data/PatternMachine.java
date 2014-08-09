package org.numenta.nupic.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Utilities for generating and manipulating patterns, for use in
 * experimentation and tests.
 * 
 * @author David Ray
 *
 * @see ConsecutivePatternMachine
 * @see SequenceMachine
 */
public class PatternMachine {
	protected int numPatterns = 100;
	protected int seed = 42;
	
	protected int n;
	protected int w;
	
	protected Random random;
	
	protected Map<Integer, LinkedHashSet<Integer>> patterns;
	
	/**
	 * @param n   Number of available bits in pattern
     * @param w   Number of on bits in pattern
     * @param num Number of available patterns
	 * 
	 * Constructs a new {@code PatternMachine}
	 */
	public PatternMachine(int n, int w) {
		this.n = n;
		this.w = w;
		random = new Random(seed);
		patterns = new LinkedHashMap<Integer, LinkedHashSet<Integer>>();
		
		generate();
	}
	
	/**
	 * Instructs the PatternMachine to construct the internal patterns.
	 */
	public void generate() {
		LinkedHashSet<Integer> pattern;
		for(int i = 0;i < numPatterns;i++) {
			pattern = xrange(n, w);
			patterns.put(i, pattern);
		}
	}
	
	/**
	 * Returns the ordered set of indices of on bits.
	 * 
	 * @param key
	 * @return
	 */
	public LinkedHashSet<Integer> get(int key) {
		return patterns.get(key);
	}
	
	/**
	 * Returns a {@link Set} of indexes mapped to patterns
	 * which contain the specified bit.
	 * 
	 * @param bit
	 * @return
	 */
	public LinkedHashSet<Integer> numbersForBit(int bit) {
		LinkedHashSet<Integer> retVal = new LinkedHashSet<Integer>();
		for(int i = 0;i < n;i++) {
			if(get(i).contains(bit)) {
				retVal.add(i);
			}
		}
		
		return retVal;
	}
	
	/**
	 * Return a map from number to matching on bits,
     * for all numbers that match a set of bits.
     * 
	 * @param bits
	 * @return
	 */
	public Map<Integer, Set<Integer>> numberMapForBits(Set<Integer> bits) {
		Map<Integer, Set<Integer>> numberMap = new LinkedHashMap<Integer, Set<Integer>>();
		
		for(Integer bit : bits) {
			Set<Integer> numbers = numbersForBit(bit);
			
			for(Integer number : numbers) {
				Set<Integer> set = null;
				if((set = numberMap.get(number)) == null) {
					numberMap.put(number, set = new LinkedHashSet<Integer>());
				}
				set.add(number);
			}
		}
		
		return numberMap;
	}
	
	public String prettyPrintPattern(Set<Integer> bits, int verbosity) {
		Map<Integer, Set<Integer>> numberMap = numberMapForBits(bits);
		String text = null;
		List<String> numberList = new ArrayList<String>();
		LinkedHashMap<Integer, LinkedHashSet<Integer>> numberItems = sortedMap(numberMap);
		for(Integer number : numberItems.keySet()) {
			String numberText = null;
			if(verbosity > 2) {
				numberText = number + " (bits: " + numberItems.get(number) + ")";
			}else if(verbosity > 1) {
				numberText = number + " (" + numberItems.get(number).size() + "bits)";
			}else{
				numberText = "" + number;
			}
			
			numberList.add(numberText);
		}
		
		text = numberList.toString();
		return text;
	}
	
	/**
	 * Returns a sorted map whose set entries are also sorted.
	 * 
	 * @param map
	 * @return
	 */
	public LinkedHashMap<Integer, LinkedHashSet<Integer>> sortedMap(Map<Integer, Set<Integer>> map) {
		LinkedHashMap<Integer, LinkedHashSet<Integer>> retVal = new LinkedHashMap<Integer, LinkedHashSet<Integer>>();
		
		List<Integer> sortByKeys = new ArrayList<Integer>(map.keySet());
		Collections.sort(sortByKeys);
		for(Integer key : sortByKeys) {
			List<Integer> sortByEntries = new ArrayList<Integer>(map.get(key));
			Collections.sort(sortByEntries);
			retVal.put(key, new LinkedHashSet<Integer>(sortByEntries));
		}
		
		return retVal;
	}
	
	/**
	 * Returns an ordered {@link Set} of Integers starting at 
	 * the specified number (start), and ending with the specified
	 * number (upperBounds).
	 * 
	 * @param start
	 * @param upperBounds
	 * @return
	 */
	public LinkedHashSet<Integer> xrange(int start, int upperBounds) {
		LinkedHashSet<Integer> retVal = new LinkedHashSet<Integer>();
		for(int i = start;i < upperBounds;i++) {
			retVal.add(i);
		}
		return retVal;
	}
	
	/**
	 * Returns a {@link Set} of numbers whose size equals the 
	 * number num and whose value is less than or equal "population".
	 * 
	 * @param random
	 * @param population
	 * @param num
	 * @return
	 */
	public Set<Integer> sample(Random random, int population, int num) {
		Set<Integer> retVal = new LinkedHashSet<Integer>();
		int count = 0;
		while(count <= num) {
			retVal.add(random.nextInt(population));
			count = retVal.size();
		}
		return retVal;
	}
	
	
}
