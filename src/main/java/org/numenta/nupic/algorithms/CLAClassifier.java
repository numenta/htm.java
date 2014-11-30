package org.numenta.nupic.algorithms;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Deque;
import org.numenta.nupic.util.Tuple;

/**
 * A CLA classifier accepts a binary input from the level below (the
 * "activationPattern") and information from the sensor and encoders (the
 * "classification") describing the input to the system at that time step.
 *
 * When learning, for every bit in activation pattern, it records a history of the
 * classification each time that bit was active. The history is weighted so that
 * more recent activity has a bigger impact than older activity. The alpha
 * parameter controls this weighting.
 *
 * For inference, it takes an ensemble approach. For every active bit in the
 * activationPattern, it looks up the most likely classification(s) from the
 * history stored for that bit and then votes across these to get the resulting
 * classification(s).
 *
 * This classifier can learn and infer a number of simultaneous classifications
 * at once, each representing a shift of a different number of time steps. For
 * example, say you are doing multi-step prediction and want the predictions for
 * 1 and 3 time steps in advance. The CLAClassifier would learn the associations
 * between the activation pattern for time step T and the classifications for
 * time step T+1, as well as the associations between activation pattern T and
 * the classifications for T+3. The 'steps' constructor argument specifies the
 * list of time-steps you want.
 * 
 * @author David Ray
 * @see BitHistory
 */
public class CLAClassifier {
	int verbosity = 0;
	/**
	 * The alpha used to compute running averages of the bucket duty
     * cycles for each activation pattern bit. A lower alpha results
     * in longer term memory.
	 */
	double alpha = 0.001;
	double actValueAlpha = 0.3;
	/** 
	 * The bit's learning iteration. This is updated each time store() gets
     * called on this bit.
	 */
	private int learnIteration;
	/**
	 * This contains the offset between the recordNum (provided by caller) and
     * learnIteration (internal only, always starts at 0).
	 */
	private int recordNumMinusLearnIteration = -1;
	/**
	 * This contains the value of the highest bucket index we've ever seen
     * It is used to pre-allocate fixed size arrays that hold the weights of
     * each bucket index during inference 
	 */
	private int maxBucketIdx;
	/** The sequence different steps of multi-step predictions */
	private TIntList steps = new TIntArrayList();
	/**
	 * History of the last _maxSteps activation patterns. We need to keep
     * these so that we can associate the current iteration's classification
     * with the activationPattern from N steps ago
	 */
	private Deque<Tuple> patternNZHistory;
	/**
	 * These are the bit histories. Each one is a BitHistory instance, stored in
     * this dict, where the key is (bit, nSteps). The 'bit' is the index of the
     * bit in the activation pattern and nSteps is the number of steps of
     * prediction desired for that bit.
	 */
	private Map<Tuple, BitHistory> activeBitHistory = new HashMap<Tuple, BitHistory>();
	/**
	 * This keeps track of the actual value to use for each bucket index. We
     * start with 1 bucket, no actual value so that the first infer has something
     * to return
	 */
	private List<Object> actualValues = new ArrayList<Object>();
	
	private String g_debugPrefix = "CLAClassifier";
	
	
	/**
	 * CLAClassifier no-arg constructor with defaults
	 */
	public CLAClassifier() {
		this(new TIntArrayList(new int[] { 1 }), 0.001, 0.3, 0);
	}
	
	/**
	 * Constructor for the CLA classifier
	 * 
	 * @param steps				sequence of the different steps of multi-step predictions to learn
	 * @param alpha				The alpha used to compute running averages of the bucket duty
               					cycles for each activation pattern bit. A lower alpha results
               					in longer term memory.
	 * @param actValueAlpha
	 * @param verbosity			verbosity level, can be 0, 1, or 2
	 */
	public CLAClassifier(TIntList steps, double alpha, double actValueAlpha, int verbosity) {
		this.steps = steps;
		this.alpha = alpha;
		this.actValueAlpha = actValueAlpha;
		this.verbosity = verbosity;
		actualValues.add(null);
		patternNZHistory = new Deque<Tuple>(steps.size() + 1);
	}
	
	/**
	 * Process one input sample.
     * This method is called by outer loop code outside the nupic-engine. We
     * use this instead of the nupic engine compute() because our inputs and
     * outputs aren't fixed size vectors of reals.
     * 
	 * @param recordNum			Record number of this input pattern. Record numbers should
     *           				normally increase sequentially by 1 each time unless there
     *           				are missing records in the dataset. Knowing this information
     *           				insures that we don't get confused by missing records.
	 * @param classification	{@link Map} of the classification information:
     *                 			bucketIdx: index of the encoder bucket
     *                 			actValue:  actual value going into the encoder
	 * @param patternNZ			list of the active indices from the output below
	 * @param learn				if true, learn this sample
	 * @param infer				if true, perform inference
	 * 
	 * @return					dict containing inference results, there is one entry for each
     *           				step in self.steps, where the key is the number of steps, and
     *           				the value is an array containing the relative likelihood for
     *           				each bucketIdx starting from bucketIdx 0.
	 *
     *           				There is also an entry containing the average actual value to
     *           				use for each bucket. The key is 'actualValues'.
	 *
     *           				for example:
     *             				{	
     *             					1 :             [0.1, 0.3, 0.2, 0.7],
     *              				4 :             [0.2, 0.4, 0.3, 0.5],
     *              				'actualValues': [1.5, 3,5, 5,5, 7.6],
     *             				}
	 */
	public Map<Object, Object> compute(int recordNum, Map<String, Object> classification, int[] patternNZ, boolean learn, boolean infer) {
		Map<Object, Object> retVal = new LinkedHashMap<Object, Object>();
		
		// Save the offset between recordNum and learnIteration if this is the first
	    // compute
		if(recordNumMinusLearnIteration == -1) {
			recordNumMinusLearnIteration = recordNum - learnIteration;
		}
		
		// Update the learn iteration
		learnIteration = recordNum - recordNumMinusLearnIteration;
		
		if(verbosity >= 1) {
			System.out.println(String.format("\n%s: compute ", g_debugPrefix));
			System.out.println(" recordNum: " + recordNum);
			System.out.println(" learnIteration: " + learnIteration);
			System.out.println(String.format(" patternNZ(%d): ", patternNZ.length, patternNZ));
			System.out.println(" classificationIn: " + classification);
		}
		
		patternNZHistory.append(new Tuple(2, learnIteration, patternNZ));
		System.out.println("deque size = " + learnIteration + "  " + patternNZHistory);
		
		//------------------------------------------------------------------------
	    // Inference:
	    // For each active bit in the activationPattern, get the classification
	    // votes
		//
		// Return value dict. For buckets which we don't have an actual value
	    // for yet, just plug in any valid actual value. It doesn't matter what
	    // we use because that bucket won't have non-zero likelihood anyways.
		if(infer) {
			// NOTE: If doing 0-step prediction, we shouldn't use any knowledge
		    //		 of the classification input during inference.
			Object defaultValue = null;
			if(steps.get(0) == 0) {
				defaultValue = 0;
			}else{
				defaultValue = classification.get("actValue");
			}
			
			Object[] actValues = new Object[this.actualValues.size()];
			for(int i = 0;i < actualValues.size();i++) {
				actValues[i] = actualValues.get(i) == null ? defaultValue : actualValues.get(i);
			}
			
			retVal.put("actualValues", actValues);
			
			// For each n-step prediction...
			for(int nSteps : steps.toArray()) {
				// Accumulate bucket index votes and actValues into these arrays
				double[] sumVotes = new double[maxBucketIdx + 1];
				double[] bitVotes = new double[maxBucketIdx + 1];
				
				for(int bit : patternNZ) {
					Tuple key = new Tuple(2, bit, nSteps);
					BitHistory history = activeBitHistory.get(key);
					if(history == null) continue;
					
					history.infer(learnIteration, bitVotes);
					
					sumVotes = ArrayUtils.d_add(sumVotes, bitVotes);
				}
				
				// Return the votes for each bucket, normalized
				double total = ArrayUtils.sum(sumVotes);
				if(total > 0) {
					sumVotes = ArrayUtils.divide(sumVotes, total);
				}else{
					// If all buckets have zero probability then simply make all of the
			        // buckets equally likely. There is no actual prediction for this
			        // timestep so any of the possible predictions are just as good.
					if(sumVotes.length > 0) {
						Arrays.fill(sumVotes, 1);
						sumVotes = ArrayUtils.divide(sumVotes, sumVotes.length);
					}
				}
				
				retVal.put(nSteps, sumVotes);
			}
		}
		
		// ------------------------------------------------------------------------
	    // Learning:
	    // For each active bit in the activationPattern, store the classification
	    // info. If the bucketIdx is None, we can't learn. This can happen when the
	    // field is missing in a specific record.
		if(learn && classification.get("bucketIdx") != null) {
			// Get classification info
			int bucketIdx = (int)classification.get("bucketIdx");
			Object actValue = classification.get("actValue");
			
			// Update maxBucketIndex
			maxBucketIdx = (int) Math.max(maxBucketIdx, bucketIdx);
			
			// Update rolling average of actual values if it's a scalar. If it's
		    // not, it must be a category, in which case each bucket only ever
		    // sees one category so we don't need a running average.
			while(maxBucketIdx > actualValues.size() - 1) {
				actualValues.add(null);
			}
			if(actualValues.get(bucketIdx) == null) {
				actualValues.set(bucketIdx, actValue);
			}else{
				if(Number.class.isAssignableFrom(actValue.getClass())) {
					actualValues.set(bucketIdx, (1.0 - actValueAlpha) * ((Number)actualValues.get(bucketIdx)).doubleValue() + 
						actValueAlpha * ((Number)actValue).doubleValue());
				}else{
					actualValues.set(bucketIdx, actValue);
				}
			}
			
			// Train each pattern that we have in our history that aligns with the
		    // steps we have in self.steps
			int nSteps = -1;
			int iteration = 0;
			int[] learnPatternNZ = null;
			for(int n : steps.toArray()) {
				nSteps = n;
				// Do we have the pattern that should be assigned to this classification
		        // in our pattern history? If not, skip it
				boolean found = false;
				for(Tuple t : patternNZHistory) {
					iteration = (int)t.get(0);
					learnPatternNZ = (int[]) t.get(1);
					if(iteration == learnIteration - nSteps) {
						found = true;
						break;
					}
					iteration++;
				}
				if(!found) continue;
				
				// Store classification info for each active bit from the pattern
		        // that we got nSteps time steps ago.
				for(int bit : learnPatternNZ) {
					// Get the history structure for this bit and step
					Tuple key = new Tuple(2, bit, nSteps);
					BitHistory history = activeBitHistory.get(key);
					if(history == null) {
						activeBitHistory.put(key, history = new BitHistory(this, bit, nSteps));
					}
					history.store(learnIteration, bucketIdx);
				}
			}
		}
		
		if(infer && verbosity >= 1) {
			System.out.println(" inference: combined bucket likelihoods:");
			System.out.println("   actual bucket values: " + retVal.get("actualValues"));
			
			for(Object key : retVal.keySet()) {
				if(key.equals("actualValues")) continue;
				
				System.out.println(String.format("  %d steps: ", key, pFormatArray((double[])retVal.get(key))));
				int bestBucketIdx = ArrayUtils.argmax((double[])retVal.get(key));
				System.out.println(String.format("   most likely bucket idx: %d, value: %s ", bestBucketIdx, 
					((Object[])retVal.get("actualValues"))[bestBucketIdx]));
				
			}
		}
		
		return retVal;
	}
	
	/**
	 * Return a string with pretty-print of an array using the given format
  	 * for each element
  	 * 
	 * @param arr
	 * @return
	 */
	private String pFormatArray(double[] arr) {
		StringBuilder sb = new StringBuilder("[ ");
		for(double d : arr) {
			sb.append(String.format("%.2f", d));
		}
		sb.append(" ]");
		return sb.toString();
	}
}
