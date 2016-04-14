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
package org.numenta.nupic.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Encodes a list of discrete categories (described by strings), that aren't
 * related to each other.
 * Each  encoding is an SDR in which w out of n bits are turned on.
 * <p/>
 * Unknown categories are encoded as a single
 *
 * @see Encoder
 * @see Encoding
 */
public class SDRCategoryEncoder extends Encoder<String> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SDRCategoryEncoder.class);

    private Random random;
    private int thresholdOverlap;
    private final SDRByCategoryMap sdrByCategory = new SDRByCategoryMap();

    /**
     * Inner class for keeping Categories and SDRs in ordered way
     */
    @SuppressWarnings("serial")
	private static final class SDRByCategoryMap extends LinkedHashMap<String, int[]> {

        public int[] getSdr(int index) {
            Map.Entry<String, int[]> entry = this.getEntry(index);
            if (entry == null) return null;
            return entry.getValue();
        }

        public String getCategory(int index) {
            Map.Entry<String, int[]> entry = this.getEntry(index);
            if (entry == null) return null;
            return entry.getKey();
        }

        public int getIndexByCategory(String category) {
            Set<String> categories = this.keySet();
            int inx = 0;
            for (String s : categories) {
                if (s.equals(category)) {
                    return inx;
                }
                inx++;
            }
            return 0;
        }

        private Map.Entry<String, int[]> getEntry(int i) {
            Set<Map.Entry<String, int[]>> entries = entrySet();
            if (i < 0 || i > entries.size()) {
                throw new IllegalArgumentException("Index should be in following range:[0," + entries.size() + "]");
            }
            int j = 0;
            for (Map.Entry<String, int[]> entry : entries)
                if (j++ == i) return entry;

            return null;

        }

    }

    /**
     * Returns a builder for building {@code SDRCategoryEncoder}s.
     * This is the only way to instantiate {@code SDRCategoryEncoder}
     *
     * @return a {@code SDRCategoryEncoder.Builder}
     */
    public static SDRCategoryEncoder.Builder builder() {
        return new Builder();
    }

    private SDRCategoryEncoder() {
    }

    /* Python mapping
    def __init__(self, n, w, categoryList = None, name="category", verbosity=0,
               encoderSeed=1, forced=False):
    */
    private void init(int n, int w, List<String> categoryList, String name,
                      int encoderSeed, boolean forced) {

        /*Python ref: n is  total bits in output
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
        double averageOverlap = w * density;
        /*
        # We can do a better job of calculating the threshold. For now, just
        # something quick and dirty, which is the midway point between average
        # and full overlap. averageOverlap is always < w,  so the threshold
        # is always < w.
        */
        this.thresholdOverlap = (int)(averageOverlap + this.w) / 2;
        /*
        #  1.25 -- too sensitive for decode test, so make it less sensitive
        */
        if (this.thresholdOverlap < this.w - 3) {
            this.thresholdOverlap = this.w - 3;
        }
        this.description.add(new Tuple(name, 0));
        this.name = name;
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
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getWidth() {
        return this.getN();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDelta() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeIntoArray(String input, int[] output) {
        int index;
        if (input == null || input.isEmpty()) {
            Arrays.fill(output, 0);
            index = 0;
        } else {
            index = getBucketIndices(input)[0];
            int[] categoryEncoding = sdrByCategory.getSdr(index);
            System.arraycopy(categoryEncoding, 0, output, 0, categoryEncoding.length);
        }
        LOG.trace("input:" + input + ", index:" + index + ", output:" + ArrayUtils.intArrayToString(output));
        LOG.trace("decoded:" + decodedToStr(decode(output, "")));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Set<FieldMetaType> getDecoderOutputFieldTypes() {
        return new HashSet<>(Arrays.asList(FieldMetaType.LIST, FieldMetaType.STRING));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getBucketIndices(String input) {
        return new int[]{(int)getScalars(input).get(0)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <S> TDoubleList getScalars(S input) {
        String inputCasted = (String)input;
        int index = 0;
        TDoubleList result = new TDoubleArrayList();
        if (inputCasted == null || inputCasted.isEmpty()) {
            result.add(0);
            return result;
        }
        if (!sdrByCategory.containsKey(input)) {
            if (isEncoderLearningEnabled()) {
                index = sdrByCategory.size();
                addCategory(inputCasted);
            }
        } else {
            index = sdrByCategory.getIndexByCategory(inputCasted);
        }
        result.add(index);
        return result;
    }


    /**
     * No parentFieldName parameter method overload for the {@link #decode(int[], String)}.
     *
     * @param encoded - bit array to be decoded
     * @return
     */
    public DecodeResult decode(int[] encoded) {
        return decode(encoded, null);
    }

    /**
     * {@inheritDoc}
     */
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
        int[] overlap = new int[sdrByCategory.size()];
        for (int i = 0; i < sdrByCategory.size(); i++) {
            int[] sdr = sdrByCategory.getSdr(i);
            for (int j = 0; j < sdr.length; j++) {
                if (sdr[j] == encoded[j] && encoded[j] == 1) {
                    overlap[i]++;
                }
            }
        }
        LOG.trace("Overlaps for decoding:");
        if (LOG.isTraceEnabled()){
            int inx = 0;
            for (String category : sdrByCategory.keySet()) {
                LOG.trace(overlap[inx] + " " + category);
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
        List<MinMax> resultRanges = new ArrayList<>();
        String fieldName;
        for (int index : matchingCategories) {
            if (resultString.length() != 0) {
                resultString.append(" ");
            }
            resultString.append(sdrByCategory.getCategory(index));
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


    /**
     * {@inheritDoc}
     */
    @Override
    public List<Encoding> topDownCompute(int[] encoded) {
        if (sdrByCategory.size() == 0) {
            return new ArrayList<>();
        }
        //TODO the rightVecProd method belongs to SparseBinaryMatrix in Nupic Core, In python this method call stack: topDownCompute [sdrcategory.py:317]/rightVecProd [math.py:4474] -->return _math._SparseMatrix32_rightVecProd(self, *args)
        int categoryIndex = ArrayUtils.argmax(rightVecProd(getTopDownMapping(), encoded));
        return getEncoderResultsByIndex(getTopDownMapping(), categoryIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Encoding> getBucketInfo(int[] buckets) {
        if (sdrByCategory.size() == 0) {
            return new ArrayList<>();
        }
        int categoryIndex = buckets[0];
        return getEncoderResultsByIndex(getTopDownMapping(), categoryIndex);
    }

    /**
     * Return the internal topDownMapping matrix used for handling the
     * {@link #getBucketInfo(int[])}  and {@link #topDownCompute(int[])} methods. This is a matrix, one row per
     * category (bucket) where each row contains the encoded output for that
     * category.
     *
     * @return {@link SparseObjectMatrix}
     */
    public SparseObjectMatrix<int[]> getTopDownMapping() {
        if (topDownMapping == null) {
            topDownMapping = new SparseObjectMatrix<>(
                    new int[]{sdrByCategory.size()});
            int[] outputSpace = new int[getN()];
            Set<String> categories = sdrByCategory.keySet();
            int inx = 0;
            for (String category : categories) {
                encodeIntoArray(category, outputSpace);
                topDownMapping.set(inx, Arrays.copyOf(outputSpace, outputSpace.length));
                inx++;
            }
        }
        return topDownMapping;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    public <S> List<S> getBucketValues(Class<S> returnType) {
        return new ArrayList<>((Collection<S>)this.sdrByCategory.keySet());
    }

    /**
     * Returns list of registered SDRs for this encoder
     *
     * @return {@link Collection}
     */
    public Collection<int[]> getSDRs() {
        return Collections.unmodifiableCollection(sdrByCategory.values());
    }


    private List<Encoding> getEncoderResultsByIndex(SparseObjectMatrix<int[]> topDownMapping, int categoryIndex) {
        List<Encoding> result = new ArrayList<>();
        String category = sdrByCategory.getCategory(categoryIndex);
        int[] encoding = topDownMapping.getObject(categoryIndex);
        result.add(new Encoding(category, categoryIndex, encoding));
        return result;
    }

    private void addCategory(String category) {
        if (this.sdrByCategory.containsKey(category)) {
            throw new IllegalArgumentException(String.format("Attempt to add encoder category '%s' that already exists",
                                                             category));
        }
        sdrByCategory.put(category, newRep());
        //reset topDown mapping
        topDownMapping = null;
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
            for (int[] existingSdr : this.sdrByCategory.values()) {
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
                                                     sdrByCategory.size(), maxAttempts));
        }
        return sdr;
    }

    /**
     * Builder class for {@code SDRCategoryEncoder}
     * <p>N is  total bits in output</p>
     * <p>W is the number of bits that are turned on for each rep</p>
     * <p>categoryList is a list of strings that define the categories.If no categories provided, then they will automatically be added as they are encountered.</p>
     * <p>forced (default false) : if true, skip checks for parameters settings</p>
     */
    public static final class Builder extends Encoder.Builder<Builder, SDRCategoryEncoder> {
        private List<String> categoryList = new ArrayList<>();
        private int encoderSeed = 1;

        @Override
        public SDRCategoryEncoder build() {
            if (n == 0) {
                throw new IllegalStateException("\"N\" should be set");
            }
            if (w == 0) {
                throw new IllegalStateException("\"W\" should be set");
            }
            if(categoryList == null) {
                throw new IllegalStateException("Category List cannot be null");
            }
            SDRCategoryEncoder sdrCategoryEncoder = new SDRCategoryEncoder();
            sdrCategoryEncoder.init(n, w, categoryList, name, encoderSeed, forced);
            
            return sdrCategoryEncoder;
        }

        public Builder categoryList(List<String> categoryList) {
            this.categoryList = categoryList;
            return this;
        }

        public Builder encoderSeed(int encoderSeed) {
            this.encoderSeed = encoderSeed;
            return this;
        }

        @Override
        public Builder radius(double radius) {
            throw new IllegalArgumentException("Not supported for this SDRCategoryEncoder");
        }

        @Override
        public Builder resolution(double resolution) {
            throw new IllegalArgumentException("Not supported for this SDRCategoryEncoder");
        }

        @Override public Builder periodic(boolean periodic) {
            throw new IllegalArgumentException("Not supported for this SDRCategoryEncoder");
        }

        @Override
        public Builder clipInput(boolean clipInput) {
            throw new IllegalArgumentException("Not supported for this SDRCategoryEncoder");
        }

        @Override
        public Builder maxVal(double maxVal) {
            throw new IllegalArgumentException("Not supported for this SDRCategoryEncoder");
        }

        @Override
        public Builder minVal(double minVal) {
            throw new IllegalArgumentException("Not supported for this SDRCategoryEncoder");
        }
    }
}
