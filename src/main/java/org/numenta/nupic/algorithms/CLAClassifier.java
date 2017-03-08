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

package org.numenta.nupic.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Deque;
import org.numenta.nupic.util.Tuple;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

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
 * @author Numenta
 * @author David Ray
 * @see BitHistory
 */
public class CLAClassifier implements Persistable, Classifier {
    private static final long serialVersionUID = 1L;

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
    int learnIteration;
    /**
     * This contains the offset between the recordNum (provided by caller) and
     * learnIteration (internal only, always starts at 0).
     */
    int recordNumMinusLearnIteration = -1;
    /**
     * This contains the value of the highest bucket index we've ever seen
     * It is used to pre-allocate fixed size arrays that hold the weights of
     * each bucket index during inference 
     */
    int maxBucketIdx;
    /** The sequence different steps of multi-step predictions */
    TIntList steps = new TIntArrayList();
    /**
     * History of the last _maxSteps activation patterns. We need to keep
     * these so that we can associate the current iteration's classification
     * with the activationPattern from N steps ago
     */
    Deque<Tuple> patternNZHistory;
    /**
     * These are the bit histories. Each one is a BitHistory instance, stored in
     * this dict, where the key is (bit, nSteps). The 'bit' is the index of the
     * bit in the activation pattern and nSteps is the number of steps of
     * prediction desired for that bit.
     */
    Map<Tuple, BitHistory> activeBitHistory = new HashMap<Tuple, BitHistory>();
    /**
     * This keeps track of the actual value to use for each bucket index. We
     * start with 1 bucket, no actual value so that the first infer has something
     * to return
     */
    List<?> actualValues = new ArrayList<Object>();

    String g_debugPrefix = "CLAClassifier";


    /**
     * CLAClassifier no-arg constructor with defaults
     */
    public CLAClassifier() {
        this(new TIntArrayList(new int[] { 1 }), 0.001, 0.3, 0);
    }

    /**
     * Constructor for the CLA classifier
     * 
     * @param steps             sequence of the different steps of multi-step predictions to learn
     * @param alpha             The alpha used to compute running averages of the bucket duty
                                cycles for each activation pattern bit. A lower alpha results
                                in longer term memory.
     * @param actValueAlpha
     * @param verbosity         verbosity level, can be 0, 1, or 2
     */
    public CLAClassifier(TIntList steps, double alpha, double actValueAlpha, int verbosity) {
        this.steps = steps;
        this.alpha = alpha;
        this.actValueAlpha = actValueAlpha;
        this.verbosity = verbosity;
        actualValues.add(null);
        patternNZHistory = new Deque<Tuple>(ArrayUtils.max(steps.toArray()) + 1);
    }

    /**
     * Process one input sample.
     * This method is called by outer loop code outside the nupic-engine. We
     * use this instead of the nupic engine compute() because our inputs and
     * outputs aren't fixed size vectors of reals.
     * 
     * @param recordNum         Record number of this input pattern. Record numbers should
     *                          normally increase sequentially by 1 each time unless there
     *                          are missing records in the dataset. Knowing this information
     *                          insures that we don't get confused by missing records.
     * @param classification    {@link Map} of the classification information:
     *                          bucketIdx: index of the encoder bucket
     *                          actValue:  actual value going into the encoder
     * @param patternNZ         list of the active indices from the output below
     * @param learn             if true, learn this sample
     * @param infer             if true, perform inference
     * 
     * @return                  {@link Classification} containing inference results, there is one entry for each
     *                          step in steps, where the key is the number of steps, and
     *                          the value is an array containing the relative likelihood for
     *                          each bucketIdx starting from bucketIdx 0.
     *
     *                          There is also an entry containing the average actual value to
     *                          use for each bucket. The key is 'actualValues'.
     *
     *                          for example:
     *                          {	
     *                              1 :             [0.1, 0.3, 0.2, 0.7],
     *                              4 :             [0.2, 0.4, 0.3, 0.5],
     *                              'actualValues': [1.5, 3.5, 5.5, 7.6],
     *                          }
     */
    @SuppressWarnings("unchecked")
    public <T> Classification<T> compute(int recordNum, Map<String, Object> classification, int[] patternNZ, boolean learn, boolean infer) {
        Classification<T> retVal = new Classification<T>();
        List<T> actualValues = (List<T>)this.actualValues;

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

        patternNZHistory.append(new Tuple(learnIteration, patternNZ));

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

            T[] actValues = (T[])new Object[this.actualValues.size()];
            for(int i = 0;i < actualValues.size();i++) {
                actValues[i] = (T)(actualValues.get(i) == null ? defaultValue : actualValues.get(i));
            }

            retVal.setActualValues(actValues);

            // For each n-step prediction...
            for(int nSteps : steps.toArray()) {
                // Accumulate bucket index votes and actValues into these arrays
                double[] sumVotes = new double[maxBucketIdx + 1];
                double[] bitVotes = new double[maxBucketIdx + 1];

                for(int bit : patternNZ) {
                    Tuple key = new Tuple(bit, nSteps);
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
                        Arrays.fill(sumVotes, 1.0 / (double)sumVotes.length);
                    }
                }

                retVal.setStats(nSteps, sumVotes);
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
                actualValues.set(bucketIdx, (T)actValue);
            }else{
                if(Number.class.isAssignableFrom(actValue.getClass())) {
                    Double val = ((1.0 - actValueAlpha) * ((Number)actualValues.get(bucketIdx)).doubleValue() + 
                                    actValueAlpha * ((Number)actValue).doubleValue());
                    actualValues.set(bucketIdx, (T)val);
                }else{
                    actualValues.set(bucketIdx, (T)actValue);
                }
            }

            // Train each pattern that we have in our history that aligns with the
            // steps we have in steps
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
                    Tuple key = new Tuple(bit, nSteps);
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
            System.out.println("   actual bucket values: " + Arrays.toString((T[])retVal.getActualValues()));

            for(int key : retVal.stepSet()) {
                if(retVal.getActualValue(key) == null) continue;

                Object[] actual = new Object[] { (T)retVal.getActualValue(key) };
                System.out.println(String.format("  %d steps: ", key, pFormatArray(actual)));
                int bestBucketIdx = retVal.getMostProbableBucketIndex(key);
                System.out.println(String.format("   most likely bucket idx: %d, value: %s ", bestBucketIdx, 
                                retVal.getActualValue(bestBucketIdx)));

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
    private <T> String pFormatArray(T[] arr) {
        if(arr == null) return "";

        StringBuilder sb = new StringBuilder("[ ");
        for(T t : arr) {
            sb.append(String.format("%.2s", t));
        }
        sb.append(" ]");
        return sb.toString();
    }

}
