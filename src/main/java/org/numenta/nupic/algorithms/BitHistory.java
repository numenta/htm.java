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

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.util.ArrayUtils;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Stores an activationPattern bit history.
 * 
 * @author David Ray
 * @see CLAClassifier
 */
public class BitHistory implements Persistable {
    private static final long serialVersionUID = 1L;

    /** Store reference to the classifier */
    CLAClassifier classifier;
    /** Form our "id" */
    String id;
    /**
     * Dictionary of bucket entries. The key is the bucket index, the
     * value is the dutyCycle, which is the rolling average of the duty cycle
     */
    TDoubleList stats;
    /** lastUpdate is the iteration number of the last time it was updated. */
    int lastTotalUpdate = -1;

    // This determines how large one of the duty cycles must get before each of the
    // duty cycles are updated to the current iteration.
    // This must be less than float32 size since storage is float32 size
    private static final int DUTY_CYCLE_UPDATE_INTERVAL = Integer.MAX_VALUE;


    /**
     * Package protected constructor for serialization purposes.
     */
    BitHistory() {}

    /**
     * Constructs a new {@code BitHistory}
     * 
     * @param classifier    instance of the {@link CLAClassifier} that owns us
     * @param bitNum        activation pattern bit number this history is for,
     *                      used only for debug messages
     * @param nSteps        number of steps of prediction this history is for, used
     *                      only for debug messages
     */
    public BitHistory(CLAClassifier classifier, int bitNum, int nSteps) {
        this.classifier = classifier;
        this.id = String.format("%d[%d]", bitNum, nSteps);
        this.stats = new TDoubleArrayList();
    }

    /**
     * Store a new item in our history.
     * <p>
     * This gets called for a bit whenever it is active and learning is enabled
     * <p>
     * Save duty cycle by normalizing it to the same iteration as
     * the rest of the duty cycles which is lastTotalUpdate.
     * <p>
     * This is done to speed up computation in inference since all of the duty
     * cycles can now be scaled by a single number.
     * <p>
     * The duty cycle is brought up to the current iteration only at inference and
     * only when one of the duty cycles gets too large (to avoid overflow to
     * larger data type) since the ratios between the duty cycles are what is
     * important. As long as all of the duty cycles are at the same iteration
     * their ratio is the same as it would be for any other iteration, because the
     * update is simply a multiplication by a scalar that depends on the number of
     * steps between the last update of the duty cycle and the current iteration.
     * 
     * @param iteration     the learning iteration number, which is only incremented
     *                      when learning is enabled
     * @param bucketIdx     the bucket index to store
     */
    public void store(int iteration, int bucketIdx) {
        // If lastTotalUpdate has not been set, set it to the current iteration.
        if(lastTotalUpdate == -1) {
            lastTotalUpdate = iteration;
        }

        // Get the duty cycle stored for this bucket.
        int statsLen = stats.size() - 1;
        if(bucketIdx > statsLen) {
            stats.add(new double[bucketIdx - statsLen]);
        }

        // Update it now.
        // duty cycle n steps ago is dc{-n}
        // duty cycle for current iteration is (1-alpha)*dc{-n}*(1-alpha)**(n)+alpha
        double dc = stats.get(bucketIdx);

        // To get the duty cycle from n iterations ago that when updated to the
        // current iteration would equal the dc of the current iteration we simply
        // divide the duty cycle by (1-alpha)**(n). This results in the formula
        // dc'{-n} = dc{-n} + alpha/(1-alpha)**n where the apostrophe symbol is used
        // to denote that this is the new duty cycle at that iteration. This is
        // equivalent to the duty cycle dc{-n}
        double denom = Math.pow((1.0 - classifier.alpha), (iteration - lastTotalUpdate));

        double dcNew = 0;
        if(denom > 0) dcNew = dc + (classifier.alpha / denom);

        // This is to prevent errors associated with infinite rescale if too large
        if(denom == 0 || dcNew > DUTY_CYCLE_UPDATE_INTERVAL) {
            double exp = Math.pow((1.0 - classifier.alpha), (iteration - lastTotalUpdate));
            double dcT = 0;
            for(int i = 0;i < stats.size();i++) {
				dcT = stats.get(i);
                dcT *= exp;
                stats.set(i, dcT);
            }

            // Reset time since last update
            lastTotalUpdate = iteration;

            // Add alpha since now exponent is 0
            dc = stats.get(bucketIdx) + classifier.alpha;
        } else {
            dc = dcNew;
        }

        stats.set(bucketIdx, dc);
        if(classifier.verbosity >= 2) {
            System.out.println(String.format("updated DC for %s,  bucket %d to %f", id, bucketIdx, dc));
        }
    }

    /**
     * Look up and return the votes for each bucketIdx for this bit.
     * 
     * @param iteration     the learning iteration number, which is only incremented
     *                      when learning is enabled
     * @param votes         array, initialized to all 0's, that should be filled
     *                      in with the votes for each bucket. The vote for bucket index N
     *                      should go into votes[N].
     */
    public void infer(int iteration, double[] votes) {
        // Place the duty cycle into the votes and update the running total for
        // normalization
        double total = 0;
        for(int i = 0;i < stats.size();i++) {
            double dc = stats.get(i);
            if(dc > 0.0) {
                votes[i] = dc;
                total += dc;
            }
        }

        // Experiment... try normalizing the votes from each bit
        if(total > 0) {
            double[] temp = ArrayUtils.divide(votes, total);
            for(int i = 0;i < temp.length;i++) votes[i] = temp[i];
        }

        if(classifier.verbosity >= 2) {
            System.out.println(String.format("bucket votes for %s:", id, pFormatArray(votes)));
        }
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
            sb.append(String.format("%.2f ", d));
        }
        sb.append("]");
        return sb.toString();
    }
}
