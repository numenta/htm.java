package org.numenta.nupic.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.data.FieldMetaType;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

public abstract class Encoder {
	/** Value used to represent no data */
	public static final double SENTINEL_VALUE_FOR_MISSING_DATA = Double.NaN;
	
	
	/**
	 * Should return the output width, in bits.
	 */
	public abstract int getWidth(Connections c);
	
	/**
	 * Returns true if the underlying encoder works on deltas
	 */
	public abstract boolean isDelta();
	
	/**
	 * Encodes inputData and puts the encoded value into the numpy output array,
     * which is a 1-D array of length returned by {@link Connections#getW()}.
	 *
     * Note: The numpy output array is reused, so clear it before updating it.
     * 
	 * @param c
	 * @param inputData Data to encode. This should be validated by the encoder.
     * @param output 1-D array of same length returned by {@link Connections#getW()}
     * 
	 * @return
	 */
	public abstract int[] encodeIntoArray(Connections c, double inputData, int[] output);
	
	/**
	 * Set whether learning is enabled.
	 * 
	 * @param 	c					the connections memory
	 * @param 	learningEnabled		flag indicating whether learning is enabled
	 */
	public abstract void setLearning(Connections c, boolean learningEnabled);
	
	/**
	 * This method is called by the model to set the statistics like min and
     * max for the underlying encoders if this information is available.
     * 
     * @param	c					the system state and data
     * @param	fieldName			fieldName name of the field this encoder is encoding, provided by
          							{@link MultiEncoder}	
     * @param	fieldStatistics		fieldStatistics dictionary of dictionaries with the first level being
          							the fieldName and the second index the statistic ie:
          							fieldStatistics['pounds']['min']
	 */
	public void setFieldStats(Connections c, String fieldName, Map<String, Double> fieldStatistics) {}
	
	/**
	 * Convenience wrapper for {@link #encodeIntoArray(Connections, double, int[])}
	 *  
     * @param c				the memory
	 * @param inputData		the input scalar
	 * @return	an array with the encoded representation of inputData
	 */
	public int[] encode(Connections c, double inputData) {
		int[] output = new int[c.getN()];
		encodeIntoArray(c, inputData, output);
		return output;
	}
	
	/**
	 * Return the field names for each of the scalar values returned by
     * getScalars.
     * 
	 * @param c
	 * @param parentFieldName	parentFieldName The name of the encoder which is our parent. This name
     *     						is prefixed to each of the field names within this encoder to form the
     *      					keys of the dict() in the retval.
	 * @return
	 */
	public List<String> getScalarNames(Connections c, String parentFieldName) {
		List<String> names = new ArrayList<String>();
		if(c.getEncoders() != null) {
			List<EncoderTuple> encoders = c.getEncoders(this);
			for(Tuple tuple : encoders) {
				List<String> subNames = ((Encoder)tuple.get(1)).getScalarNames(c, c.getName());
				List<String> hierarchicalNames = new ArrayList<String>();
				if(parentFieldName != null) {
					for(String name : subNames) {
						hierarchicalNames.add(String.format("%s.%s", parentFieldName, name));
					}
				}
				names.addAll(hierarchicalNames);
			}
		}else{
			if(parentFieldName != null) {
				names.add(parentFieldName);
			}else{
				names.add((String)c.getEncoderTuple(this).get(0));
			}
		}
		
		return names;
	}
	
	/**
	 * Returns a sequence of field types corresponding to the elements in the
     * decoded output field array.  The types are defined by {@link FieldMetaType}
     * 
	 * @param c 	the state memory
	 * @return
	 */
	public List<FieldMetaType> getDecoderOutputFieldTypes(Connections c) {
		if(c.getFlattenedFieldTypeList() != null) {
			return c.getFlattenedFieldTypeList();
		}
		
		List<FieldMetaType> retVal = new ArrayList<FieldMetaType>();
		for(Tuple t : c.getEncoders(this)) {
			List<FieldMetaType> subTypes = ((Encoder)t.get(1)).getDecoderOutputFieldTypes(c);
			retVal.addAll(subTypes);
		}
		c.setFlattenedFieldTypeList(retVal);
		return retVal;
	}
	
	/**
	 * Gets the value of a given field from the input record
	 * @param c				the state memory
	 * @param inputObject	input object
	 * @param fieldName		the name of the field containing the input object.
	 * @return
	 */
	public Object getInputValue(Connections c, Object inputObject, String fieldName) {
		if(Map.class.isAssignableFrom(inputObject.getClass())) {
			@SuppressWarnings("rawtypes")
			Map map = (Map)inputObject;
			if(!map.containsKey(fieldName)) {
				throw new IllegalArgumentException("Unknown field name " + fieldName +
					" known fields are: " + map.keySet() + ". ");
			}
			return map.get(fieldName);
		}
		return null;
	}
	
	/**
	 * Returns a reference to each sub-encoder in this encoder. They are
     * returned in the same order as they are for getScalarNames() and
     * getScalars()
     * 
	 * @param c
	 * @return
	 */
	public List<Encoder> getEncoderList(Connections c) {
		List<Encoder> encoders = new ArrayList<Encoder>();
		
		List<EncoderTuple> registeredList = c.getEncoders(this);
		if(registeredList != null && !registeredList.isEmpty()) {
			for(Tuple t : registeredList) {
				List<Encoder> subEncoders = ((Encoder)t.get(1)).getEncoderList(c);
				encoders.addAll(subEncoders);
			}
		}else{
			encoders.add(this);
		}
		return encoders;
	}
	
	/**
	 * Returns an {@link TDoubleList} containing the sub-field scalar value(s) for
     * each sub-field of the inputData. To get the associated field names for each of
     * the scalar values, call getScalarNames().
	 *
     * For a simple scalar encoder, the scalar value is simply the input unmodified.
     * For category encoders, it is the scalar representing the category string
     * that is passed in. For the datetime encoder, the scalar value is the
     * the number of seconds since epoch.
	 * 
     * The intent of the scalar representation of a sub-field is to provide a
     * baseline for measuring error differences. You can compare the scalar value
     * of the inputData with the scalar value returned from topDownCompute() on a
     * top-down representation to evaluate prediction accuracy, for example.
     * 
     * @param <T>  the specifically typed input object
     * 
	 * @return
	 */
	public abstract <T> TDoubleList getScalars(Connections c, T inputData);
	
	/**
	 * Returns the input in the same format as is returned by topDownCompute().
     * For most encoder types, this is the same as the input data.
     * For instance, for scalar and category types, this corresponds to the numeric
     * and string values, respectively, from the inputs. For datetime encoders, this
     * returns the list of scalars for each of the sub-fields (timeOfDay, dayOfWeek, etc.)
	 * 
     * This method is essentially the same as getScalars() except that it returns
     * strings
	 * 
     * @param inputData The input data in the format it is received from the data source
	 * 
     * @return A list of values, in the same format and in the same order as they
     * are returned by topDownCompute.
     * 
	 * @param c				the state memory
	 * @param inputData		the input data object
	 * @return	list of encoded values in String form
	 */
	public <T> List<String> getEncodedValues(Connections c, T inputData) {
		List<String> retVals = new ArrayList<String>();
		Map<EncoderTuple, List<EncoderTuple>> encoders = c.getEncoders();
		if(encoders != null && encoders.size() > 0) {
			for(EncoderTuple t : encoders.keySet()) {
				retVals.addAll(t.getEncoder().getEncodedValues(c, inputData));
			}
		}else{
			retVals.add(inputData.toString());
		}
		
		return retVals;
	}
	
	/**
	 * Returns an array containing the sub-field bucket indices for
     * each sub-field of the inputData. To get the associated field names for each of
     * the buckets, call getScalarNames().
	 * 
	 * @param 	c			the state memory
     * @param  	inputData 	The data from the source. This is typically a object with members.
     * @return 	array of bucket indices
	 */
	public int[] getBucketIndices(Connections c, double input) {
		TIntList l = new TIntArrayList();
		Map<EncoderTuple, List<EncoderTuple>> encoders = c.getEncoders();
		if(encoders != null && encoders.size() > 0) {
			for(EncoderTuple t : encoders.keySet()) {
				l.addAll(t.getEncoder().getBucketIndices(c, input));
			}
		}else{
			throw new IllegalStateException("Should be implemented in base classes that are not " +
				"containers for other encoders");
		}
		return l.toArray();
	}
	
	/**
	 * Returns an array containing the sub-field bucket indices for
     * each sub-field of the inputData. To get the associated field names for each of
     * the buckets, call getScalarNames().
	 * 
	 * @param 	c			the state memory
     * @param  	inputData 	The data from the source. This is typically a object with members.
     * @return 	array of bucket indices
	 */
	public int[] getBucketIndices(Connections c, Object input) {
		TIntList l = new TIntArrayList();
		Map<EncoderTuple, List<EncoderTuple>> encoders = c.getEncoders();
		if(encoders != null && encoders.size() > 0) {
			for(EncoderTuple t : encoders.keySet()) {
				l.addAll(t.getEncoder().getBucketIndices(c, input));
			}
		}else{
			throw new IllegalStateException("Should be implemented in base classes that are not " +
				"containers for other encoders");
		}
		return l.toArray();
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
     * @param c	the state memory
	 * @return  list of items, each item representing the bucket value for that
     *          bucket.
	 */
	public abstract TDoubleList getBucketValues(Connections c);
	
	/**
	 * Takes an encoded output and does its best to work backwards and generate
     * the input that would have generated it.
	 *
     * In cases where the encoded output contains more ON bits than an input
     * would have generated, this routine will return one or more ranges of inputs
     * which, if their encoded outputs were ORed together, would produce the
     * target output. This behavior makes this method suitable for doing things
     * like generating a description of a learned coincidence in the SP, which
     * in many cases might be a union of one or more inputs.
	 * 
     * If instead, you want to figure the *most likely* single input scalar value
     * that would have generated a specific encoded output, use the topDownCompute()
     * method.
	 * 
     * If you want to pretty print the return value from this method, use the
     * decodedToStr() method.
	 *
	 *************
	 * OUTPUT EXPLAINED:
	 * 
     * fieldsMap is a {@link Map} where the keys represent field names
     * (only 1 if this is a simple encoder, > 1 if this is a multi
     * or date encoder) and the values are the result of decoding each
     * field. If there are  no bits in encoded that would have been
     * generated by a field, it won't be present in the Map. The
     * key of each entry in the dict is formed by joining the passed in
     * parentFieldName with the child encoder name using a '.'.
	 * 
     * Each 'value' in fieldsMap consists of a {@link Tuple} of (ranges, desc), 
     * where ranges is a list of one or more {@link MinMax} ranges of
     * input that would generate bits in the encoded output and 'desc'
     * is a comma-separated pretty print description of the ranges. 
     * For encoders like the category encoder, the 'desc' will contain 
     * the category names that correspond to the scalar values included 
     * in the ranges.
	 * 
     * The fieldOrder is a list of the keys from fieldsMap, in the
     * same order as the fields appear in the encoded output.
     * 
     * Example retvals for a scalar encoder:
	 *
     *   {'amount':  ( [[1,3], [7,10]], '1-3, 7-10' )}
     *   {'amount':  ( [[2.5,2.5]],     '2.5'       )}
 	 *
     * Example retval for a category encoder:
	 *
     *   {'country': ( [[1,1], [5,6]], 'US, GB, ES' )}
	 *
     * Example retval for a multi encoder:
	 *
     *   {'amount':  ( [[2.5,2.5]],     '2.5'       ),
     *   'country': ( [[1,1], [5,6]],  'US, GB, ES' )}
     *    
	 * @param c					the memory
	 * @param encoded      		The encoded output that you want decode
     * @param parentFieldName 	The name of the encoder which is our parent. This name
     *      					is prefixed to each of the field names within this encoder to form the
     *    						keys of the {@link Map} returned.
	 *
     * @returns Tuple(fieldsMap, fieldOrder)
	 */
	@SuppressWarnings("unchecked")
	public Tuple decode(Connections c, int[] encoded, String parentFieldName) {
		Map<String, Tuple> fieldsMap = new HashMap<String, Tuple>();
		List<String> fieldsOrder = new ArrayList<String>();
		
		String parentName = parentFieldName == null || parentFieldName.isEmpty() ? 
			c.getName() : String.format("%s.%s", parentFieldName, c.getName());
		
		List<EncoderTuple> encoders = c.getEncoders(this);
		int len = encoders.size();
		for(int i = 0;i < len;i++) {
			Tuple threeFieldsTuple = encoders.get(i);
			int nextOffset = 0;
			if(i < len - 1) {
				nextOffset = (Integer)encoders.get(i + 1).get(2);
			}else{
				nextOffset = c.getW();
			}
			
			int[] fieldOutput = ArrayUtils.sub(encoded, ArrayUtils.range((Integer)threeFieldsTuple.get(2), nextOffset));
			
			Tuple result = ((Encoder)threeFieldsTuple.get(1)).decode(c, fieldOutput, parentName);
			
			fieldsMap.putAll((Map<String, Tuple>)result.get(0));
			fieldsOrder.addAll((List<String>)result.get(1));
		}
		
		return new Tuple(2, fieldsMap, fieldsOrder);
	}
	
	/**
	 * This returns a list of tuples, each containing (name, offset).
     * The 'name' is a string description of each sub-field, and offset is the bit
     * offset of the sub-field for that encoder.
	 * 
     * For now, only the 'multi' and 'date' encoders have multiple (name, offset)
     * pairs. All other encoders have a single pair, where the offset is 0.
     * 
	 * @param c		the connections memory
	 * @return		list of tuples, each containing (name, offset)
	 */		
	public abstract List<Tuple> getDescription(Connections c); 
	
	/**
	 * Return a pretty print string representing the return value from decode().
	 * 
	 * @param decodeResults
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String decodedToStr(Tuple decodeResults) {
		StringBuilder desc = new StringBuilder();
		Map<String, Tuple> fieldsDict = (Map<String, Tuple>)decodeResults.get(0);
		List<String> fieldsOrder = (List<String>)decodeResults.get(1);
		for(String fieldName : fieldsOrder) {
			Tuple ranges = fieldsDict.get(fieldName);
			if(desc.length() > 0) {
				desc.append(", ").append(fieldName).append(":");
			}else{
				desc.append(fieldName).append(":");
			}
			desc.append("[").append(ranges.get(1)).append("]");
		}
		return desc.toString();
	}
	
	/**
	 * Returns a list of EncoderResult named tuples describing the top-down
     * best guess inputs for each sub-field given the encoded output. These are the
     * values which are most likely to generate the given encoded output.
     * To get the associated field names for each of the values, call
     * getScalarNames().
	 * 
     * @param encoded The encoded output. Typically received from the topDown outputs
     *              from the spatial pooler just above us.
	 *
     * @returns A list of EncoderResult named tuples. Each EncoderResult has
     *        three attributes:
	 *
     *        -# value:         This is the best-guess value for the sub-field
     *                          in a format that is consistent with the type
     *                          specified by getDecoderOutputFieldTypes().
     *                          Note that this value is not necessarily
     *                          numeric.
	 *
     *        -# scalar:        The scalar representation of this best-guess
     *                          value. This number is consistent with what
     *                          is returned by getScalars(). This value is
     *                          always an int or float, and can be used for
     *                          numeric comparisons.
	 *
     *        -# encoding       This is the encoded bit-array (numpy array)
     *                          that represents the best-guess value.
     *                          That is, if 'value' was passed to
     *                          encode(), an identical bit-array should be
     *                          returned.
	 */
	public List<EncoderResult> topDownCompute(Connections c, int[] encoded) {
		List<EncoderResult> retVals = new ArrayList<EncoderResult>();
		
		List<EncoderTuple> encoders = c.getEncoders(this);
		int len = encoders.size();
		for(int i = 0;i < len;i++) {
			int offset = (int)encoders.get(i).get(2);
			Encoder encoder = (Encoder)encoders.get(i).get(1);
			
			int nextOffset;
			if(i < len - 1) {
				//Encoders = List<Encoder> : Encoder = EncoderTuple(name, encoder, offset)
				nextOffset = (int)encoders.get(i + 1).get(2);
			}else{
				nextOffset = c.getW();
			}
			
			int[] fieldOutput = ArrayUtils.sub(encoded, ArrayUtils.range(offset, nextOffset));
			List<EncoderResult> values = encoder.topDownCompute(c, fieldOutput);
			
			retVals.addAll(values);
		}
		
		return retVals;
	}
}
