/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package org.numenta.nupic.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SDRCategoryEncoder extends Encoder<String> {
    //TODO this should be moved to  the base class
    protected List<Tuple> description = new ArrayList<>();

    private Random random;
    private double averageOverlap;
    private int thresholdOverlap;
    private TObjectIntMap<String> categoryToIndex;
    private int ncategories;
    private List<String> categories;//check uniqueness on insert
    private List<int[]> sdrs;


    public static SDRCategoryEncoder.Builder builder() {
        return new Builder();
    }

    private SDRCategoryEncoder() {
    }

    /* Python mapping
    def __init__(self, n, w, categoryList = None, name="category", verbosity=0,
               encoderSeed=1, forced=False):
    */
    private void init(int n, int w, Set<String> categoryList, String name, int verbosity,
                      int encoderSeed, boolean forced) {

    /*n is  total bits in output
    w is the number of bits that are turned on for each rep
    categoryList is a list of strings that define the categories.
    If "none" then categories will automatically be added as they are encountered.
    forced (default False) : if True, skip checks for parameters' settings; see encoders/scalar.py for details*/
        this.n = n;
        this.w = w;
        this.encLearningEnabled = true;
        this.random = new Random();
        if (encoderSeed != -1) {
            this.random.setSeed(encoderSeed);
        }
        if (!forced) {
            if (n / w < 2) {
                throw new IllegalArgumentException(String.format(
                        //TODO this message from in /nupic/nupic/encoders/sdrcategory.py::67 is not clear
                        "Number of ON bits in SDR (%d) must be much smaller than the output width (%d)", w, n));
            }
            if (w < 21) {
                throw new IllegalArgumentException(String.format(
                        "Number of bits in the SDR (%d) must be greater than 2, and should be >= 21, pass forced=True to init() to override this check",
                        w));
            }

        }
    /*
    #Calculate average overlap of SDRs for decoding
    #Density is fraction of bits on, and it is also the
    #probability that any individual bit is on.
    */
        double density = (double)this.w / this.n;
        this.averageOverlap = w * density;
    /*
    # We can do a better job of calculating the threshold. For now, just
    # something quick and dirty, which is the midway point between average
    # and full overlap. averageOverlap is always < w,  so the threshold
    # is always < w.
    */
        this.thresholdOverlap = (int)(this.averageOverlap + this.w) / 2;
    /*
    #  1.25 -- too sensitive for decode test, so make it less sensitive
    */
        if (this.thresholdOverlap < this.w - 3) {
            this.thresholdOverlap = this.w - 3;
        }
        this.verbosity = verbosity;
        this.description.add(new Tuple(2, name, 0));
        this.name = name;
        this.categoryToIndex = new TObjectIntHashMap<>();
        this.ncategories = 0;
        this.categories = new ArrayList<>();
        this.sdrs = null;
    /*
    # Always include an 'unknown' category for
    # edge cases
    */
        this.addCategory("<UNKNOWN>");
        if (categoryList == null || categoryList.size() == 0) {
            this.setLearningEnabled(true);
        } else {
            this.setLearningEnabled(false);
            for (String category : categoryList) {
                this.addCategory(category);
            }
            assert this.ncategories == categoryList.size() + 1;
        }
    /*
    # Not used by this class. Used for decoding (scalarsToStr())
    */
        //self.encoders = None
    /*
    # This matrix is used for the topDownCompute. We build it the first time
    #  topDownCompute is called
    */
        //self._topDownMappingM = None
        //self._topDownValues = None
    }

    @Override
    public int getWidth() {
        return this.getN();
    }

    @Override
    public boolean isDelta() {
        return false;
    }

    @Override
    public void encodeIntoArray(String input, int[] output) {
        int index;
        if (input == null || input.isEmpty()) {
            Arrays.fill(output, 0);
            index = 0;
        } else {
            index = getBucketIndices(input)[0];
            int[] category = sdrs.get(index);
            System.arraycopy(category, 0, output, 0, category.length);
        }
        if (verbosity >= 2) {
            System.out.println("input:" + input + ", index:" + index + ", output:" + ArrayUtils.intArrayToString(
                    output));
            System.out.println("decoded:" + decodedToStr(decode(output, "")));
        }
    }


    @Override
    public int[] getBucketIndices(String input) {
        return new int[]{(int)getScalars(input).get(0)};
    }

    @Override
    public <S> TDoubleList getScalars(S input) {
        String inputCasted = (String)input;
        int index;
        TDoubleList result = new TDoubleArrayList();
        if (inputCasted == null || inputCasted.isEmpty()) {
            result.add(0);
            return result;
        }
        if (!categoryToIndex.containsKey(input)) {
            if (isEncoderLearningEnabled()) {
                addCategory(inputCasted);
                index = ncategories - 1;
                assert index == categoryToIndex.get(input);
            } else {
                index = 0;
            }
        } else {
            index = categoryToIndex.get(input);
        }
        result.add(index);
        return result;
    }


    /*
    *No parentFieldName parameter method overload for  {@link #decode()}
    */
    public DecodeResult decode(int[] encoded) {
      return decode(encoded, null);
    }

    @Override
    public DecodeResult decode(int[] encoded, String parentFieldName) {
        //assert (encoded[0:self.n] <= 1.0).all()
        assert ArrayUtils.all(encoded, new Condition.Adapter<Integer>() {
            @Override
            public boolean eval(int i) {
                return i <= 1;
            }
        });
        //overlaps =  (self.sdrs * encoded[0:self.n]).sum(axis=1)
        int[] overlap = new int[sdrs.size()];
        for (int i = 0; i < sdrs.size(); i++) {
            int[] sdr = sdrs.get(i);
            for (int j = 0; j < sdr.length; j++) {
                if (sdr[j] == encoded[j] && encoded[j] == 1) {
                    overlap[i]++;
                }
            }
        }
        if (verbosity >= 2) {
            System.out.println("Overlaps for decoding:");
            int inx = 0;
            for (String category : categories) {
                System.out.println(overlap[inx] + " " + category);
                inx++;
            }
        }
        //matchingCategories =  (overlaps > self.thresholdOverlap).nonzero()[0]
        int[] matchingCategories = ArrayUtils.where(overlap, new Condition.Adapter<Integer>() {
            @Override
            public boolean eval(int overlaps) {
                return overlaps > thresholdOverlap;
            }
        });
        StringBuilder resultString = new StringBuilder();
        List<MinMax> resultRanges  = new ArrayList<>();
        String fieldName;
        for (int index : matchingCategories) {
            if(resultString.length() != 0){
                resultString.append(" ");
            }
            resultString.append(categories.get(index));
            resultRanges.add(new MinMax(index, index));
        }
        if (parentFieldName == null || parentFieldName.isEmpty()) {
            fieldName = getName();
        } else {
            fieldName = String.format("%s.%s", parentFieldName, getName());
        }
        Map<String, RangeList> fieldsDict = new HashMap<>();
        fieldsDict.put(fieldName, new RangeList(resultRanges, resultString.toString()));
        // return ({fieldName: (resultRanges, resultString)}, [fieldName])
        return new DecodeResult(fieldsDict, Arrays.asList(fieldName));
    }


    //TODO this code repeats in most of subclasses of Encoder, why don't we move this to Encoder as default
    @Override public void setLearning(boolean learningEnabled) {
        setLearningEnabled(learningEnabled);
    }

    //TODO this method can be default implementation in Encoder instead of abstract. We just move description variable to the base class and initialize it in init method
    @Override
    public List<Tuple> getDescription() {
        return description;
    }


    @Override public <S> List<S> getBucketValues(Class<S> returnType) {
        return null;
    }


    public void addCategory(String category) {
        if (!this.categories.add(category)) {
            throw new IllegalArgumentException(String.format("Attempt to add encoder category '%s' that already exists",
                                                             category));
        }
        if (this.sdrs == null) {
            assert ncategories == 0;
            assert categoryToIndex.size() == 0;
            sdrs = new ArrayList<>(16);
        }
        sdrs.add(newRep());
        categoryToIndex.put(category, ncategories);
        ncategories += 1;
    }

    //replacement for Python sorted(self.random.sample(xrange(self.n), self.w))
    private int[] getSortedSample(final int populationSize, final int sampleLength) {
        TIntSet resultSet = new TIntHashSet();
        while (resultSet.size() < sampleLength) {
            resultSet.add(random.nextInt(populationSize));
        }
        int[] result = resultSet.toArray();
        Arrays.sort(result);
        return result;
    }


    private int[] newRep() {
        int maxAttempts = 1000;
        boolean foundUnique = true;
        int[] oneBits;
        int sdr[] = new int[n];
        for (int index = 0; index < maxAttempts; index++) {
            foundUnique = true;
            oneBits = getSortedSample(n, w);
            sdr = new int[n];
            for (int i = 0; i < oneBits.length; i++) {
                int oneBitInx = oneBits[i];
                sdr[oneBitInx] = 1;
            }
            for (int[] existingSdr : this.sdrs) {
                if (Arrays.equals(sdr, existingSdr)) {
                    foundUnique = false;
                    break;
                }
            }
            if (foundUnique) {
                break;
            }
        }
        if (!foundUnique) {
            throw new RuntimeException(String.format("Error, could not find unique pattern %d after %d attempts",
                                                     ncategories, maxAttempts));
        }
        return sdr;
    }

    public List<int[]> getSdrs() {
        return Collections.unmodifiableList(sdrs);
    }


    public static final class Builder {
        private int n;
        private int w;
        private Set<String> categoryList = new HashSet<>();
        private String name = "category";
        private int verbosity = 0;
        private int encoderSeed = 1;
        private boolean forced = false;

        public SDRCategoryEncoder build() {
            if (n == 0) {
                throw new IllegalStateException("\"N\" should be set");
            }
            if (w == 0) {
                throw new IllegalStateException("\"W\" should be set");
            }
            SDRCategoryEncoder encoder = new SDRCategoryEncoder();
            encoder.init(n, w, categoryList, name, verbosity, encoderSeed, forced);
            return encoder;
        }

        public Builder setN(int n) {
            this.n = n;
            return this;
        }

        public Builder setW(int w) {
            this.w = w;
            return this;
        }

        public Builder setCategoryList(Set<String> categoryList) {
            this.categoryList = categoryList;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setVerbosity(int verbosity) {
            this.verbosity = verbosity;
            return this;
        }

        public Builder setEncoderSeed(int encoderSeed) {
            this.encoderSeed = encoderSeed;
            return this;
        }

        public Builder setForced(boolean forced) {
            this.forced = forced;
            return this;
        }
    }
}
