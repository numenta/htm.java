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

package org.numenta.nupic.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;

/**
 * Encodes a list of discrete categories (described by strings), that aren't
 * related to each other, so we never emit a mixture of categories.
 * 
 * The value of zero is reserved for "unknown category"
 * 
 * Internally we use a ScalarEncoder with a radius of 1, but since we only encode
 * integers, we never get mixture outputs.
 *
 * The SDRCategoryEncoder (not yet implemented in Java) uses a different method to encode categories
 * 
 * <P>
 * Typical usage is as follows:
 * <PRE>
 * CategoryEncoder.Builder builder =  ((CategoryEncoder.Builder)CategoryEncoder.builder())
 *      .w(3)
 *      .radius(0.0)
 *      .minVal(0.0)
 *      .maxVal(8.0)
 *      .periodic(false)
 *      .forced(true);
 *      
 * CategoryEncoder encoder = builder.build();
 * 
 * <b>Above values are <i>not</i> an example of "sane" values.</b>
 * 
 * </PRE>
 * 
 * @author David Ray
 * @see ScalarEncoder
 * @see Encoder
 * @see EncoderResult
 * @see Parameters
 */
public class CategoryEncoder extends Encoder<String> {
	protected int ncategories;
	
	protected TObjectIntMap<String> categoryToIndex = new TObjectIntHashMap<String>();
	protected TIntObjectMap<String> indexToCategory = new TIntObjectHashMap<String>();
	
	protected List<String> categoryList;
	
	protected int width;
	protected Tuple description;

	private ScalarEncoder scalarEncoder;
	
	/**
	 * Constructs a new {@code CategoryEncoder}
	 */
	private CategoryEncoder() {
	}
	
	/**
	 * Returns a builder for building CategoryEncoders. 
	 * This builder may be reused to produce multiple builders
	 * 
	 * @return a {@code CategoryEncoder.Builder}
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Encoder.Builder builder() {
		return new CategoryEncoder.Builder();
	}
	
	public void init() {
		// number of categories includes zero'th category: "unknown"
		ncategories = categoryList == null ? 0 : categoryList.size() + 1;
		minVal = 0;
		maxVal = ncategories - 1;
		scalarEncoder = new ScalarEncoder();
		scalarEncoder.init();

		
		indexToCategory.put(0, "<UNKNOWN>");
		if(categoryList != null && !categoryList.isEmpty()) {
			int len = categoryList.size();
			for(int i = 0;i < len;i++) {
				categoryToIndex.put(categoryList.get(i), i + 1);
				indexToCategory.put(i + 1, categoryList.get(i));
			}
		}
		
		width = n = w * ncategories;
		if(getWidth() != width) {
			throw new IllegalStateException(
				"Width != w (num bits to represent output item) * #categories");
		}
		
		description = new Tuple(2, name, 0);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public <T> TDoubleList getScalars(T d) {
		return new TDoubleArrayList(new double[] { categoryToIndex.get(d) });
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int[] getBucketIndices(String input) {
		if(input == null) return null;
		return super.getBucketIndices(categoryToIndex.get(input));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void encodeIntoArray(String input, int[] output) {
		String val = null;
		double value = 0;
		if(input == null) {
			val = "<missing>";
		}else{
			value = categoryToIndex.get(input);
			value = value == categoryToIndex.getNoEntryValue() ? 0 : value;
			scalarEncoder.encodeIntoArray(value, output);
		}
		
		if(verbosity >= 2) {
			System.out.println(
				String.format("input: %s,  val: %s, value: %d, output: %s",
					input, val, value, Arrays.toString(output)));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DecodeResult decode(int[] encoded, String parentFieldName) {
		// Get the scalar values from the underlying scalar encoder
		DecodeResult result = scalarEncoder.decode(encoded, parentFieldName);
		
		if(result.getFields().size() == 0) {
			return result;
		}
		
		// Expect only 1 field
		if(result.getFields().size() != 1) {
			throw new IllegalStateException("Expecting only one field");
		}
		
		//Get the list of categories the scalar values correspond to and
	    //  generate the description from the category name(s).
		Map<String, RangeList> fieldRanges = result.getFields();
		List<MinMax> outRanges = new ArrayList<MinMax>();
		StringBuilder desc = new StringBuilder();
		for(String descripStr : fieldRanges.keySet()) {
			MinMax minMax = fieldRanges.get(descripStr).getRange(0);
			int minV = (int)Math.round(minMax.min());
			int maxV = (int)Math.round(minMax.max());
			outRanges.add(new MinMax(minV, maxV));
			while(minV <= maxV) {
				if(desc.length() > 0) {
					desc.append(", ");
				}
				desc.append(indexToCategory.get(minV));
				minV += 1;
			}
		}
		
		//Return result
		String fieldName;
		if(!parentFieldName.isEmpty()) {
			fieldName = String.format("%s.%s", parentFieldName, name);
		}else{
			fieldName = name;
		}
		
		Map<String, RangeList> retVal = new HashMap<String, RangeList>();
		retVal.put(fieldName, new RangeList(outRanges, desc.toString()));
		
		return new DecodeResult(retVal, Arrays.asList(new String[] { fieldName }));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public TDoubleList closenessScores(TDoubleList expValues, TDoubleList actValues, boolean fractional) {
		double expValue = expValues.get(0);
		double actValue = actValues.get(0);
		
		double closeness = expValue == actValue ? 1.0 : 0;
		if(!fractional) closeness = 1.0 - closeness;
		
		return new TDoubleArrayList(new double[]{ closeness });
	}
	
	/**
	 * Returns a list of items, one for each bucket defined by this encoder.
     * Each item is the value assigned to that bucket, this is the same as the
     * EncoderResult.value that would be returned by getBucketInfo() for that
     * bucket and is in the same format as the input that would be passed to
     * encode().
	 * 
     * This call is faster than calling getBucketInfo() on each bucket individually
     * if all you need are the bucket values.
	 *
	 * @param	returnType 		class type parameter so that this method can return encoder
     * 							specific value types
     * 
     * @return list of items, each item representing the bucket value for that
     *        bucket.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> getBucketValues(Class<T> t) {
		if(bucketValues == null) {
			SparseObjectMatrix<int[]> topDownMapping = scalarEncoder.getTopDownMapping();
			int numBuckets = topDownMapping.getMaxIndex() + 1;
			bucketValues = new ArrayList<String>();
			for(int i = 0;i < numBuckets;i++) {
				((List<String>)bucketValues).add((String)getBucketInfo(new int[] { i }).get(0).getValue());
			}
		}
		
		return (List<T>)bucketValues;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<EncoderResult> getBucketInfo(int[] buckets) {
		// For the category encoder, the bucket index is the category index
		List<EncoderResult> bucketInfo = super.getBucketInfo(buckets);
		
		int categoryIndex = (int)Math.round((double)bucketInfo.get(0).getValue());
		String category = indexToCategory.get(categoryIndex);
		
		bucketInfo.set(0, new EncoderResult(category, categoryIndex, bucketInfo.get(0).getEncoding()));
		return bucketInfo;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<EncoderResult> topDownCompute(int[] encoded) {
		List<EncoderResult> encoderResult = super.topDownCompute(encoded);
		return encoderResult;
	}

    public List<String> getCategoryList() {
        return categoryList;
    }

    public void setCategoryList(List<String> categoryList) {
        this.categoryList = categoryList;
    }
    
    /**
	 * Returns a {@link EncoderBuilder} for constructing {@link CategoryEncoder}s
	 * 
	 * The base class architecture is put together in such a way where boilerplate
	 * initialization can be kept to a minimum for implementing subclasses. 
	 * Hopefully! :-)
	 * 
	 * @see ScalarEncoder.Builder#setStuff(int)
	 */
	public static class Builder extends Encoder.Builder<CategoryEncoder.Builder, CategoryEncoder> {
		private List<String> categoryList;
		
		private Builder() {}

		@Override
		public CategoryEncoder build() {
			//Must be instantiated so that super class can initialize 
			//boilerplate variables.
			encoder = new CategoryEncoder();
			
			//Call super class here
			super.build();
			
			////////////////////////////////////////////////////////
			//  Implementing classes would do setting of specific //
			//  vars here together with any sanity checking       //
			////////////////////////////////////////////////////////
			if(categoryList == null) {
				throw new IllegalStateException("Category List cannot be null");
			}
			//Set CategoryEncoder specific field
			((CategoryEncoder)encoder).setCategoryList(this.categoryList);
			//Call init
			((CategoryEncoder)encoder).init();
			
			return (CategoryEncoder)encoder;
		}
		
		/**
		 * Never called - just here as an example of specialization for a specific 
		 * subclass of Encoder.Builder
		 * 
		 * Example specific method!!
		 * 
		 * @param stuff
		 * @return
		 */
		public CategoryEncoder.Builder categoryList(List<String> categoryList) {
			this.categoryList = categoryList;
			return this;
		}
	}

	@Override
	public int getWidth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isDelta() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setLearning(boolean learningEnabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Tuple> getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
}
