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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

/**
 * <pre>
 * An encoder takes a value and encodes it with a partial sparse representation
 * of bits.  The Encoder superclass implements:
 * - encode() - returns an array encoding the input; syntactic sugar
 *   on top of encodeIntoArray. If pprint, prints the encoding to the terminal
 * - pprintHeader() -- prints a header describing the encoding to the terminal
 * - pprint() -- prints an encoding to the terminal
 *
 * Methods/properties that must be implemented by subclasses:
 * - getDecoderOutputFieldTypes()   --  must be implemented by leaf encoders; returns
 *                                      [`nupic.data.fieldmeta.FieldMetaType.XXXXX`]
 *                                      (e.g., [nupic.data.fieldmetaFieldMetaType.float])
 * - getWidth()                     --  returns the output width, in bits
 * - encodeIntoArray()              --  encodes input and puts the encoded value into the output array,
 *                                      which is a 1-D array of length returned by getWidth()
 * - getDescription()               --  returns a list of (name, offset) pairs describing the
 *                                      encoded output
 * </pre>
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
 * @author Numenta
 * @author David Ray
 */
public abstract class Encoder<T>  implements Persistable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Encoder.class);

	/** Value used to represent no data */
	public static final double SENTINEL_VALUE_FOR_MISSING_DATA = Double.NaN;
    protected List<Tuple> description = new ArrayList<>();

	/** The number of bits that are set to encode a single value - the
     * "width" of the output signal
     */
    protected int w = 0;
    /** number of bits in the representation (must be >= w) */
    protected int n = 0;
    /** the half width value */
    protected int halfWidth;
    /**
     * inputs separated by more than, or equal to this distance will have non-overlapping
     * representations
     */
    protected double radius = 0;
    /** inputs separated by more than, or equal to this distance will have different representations */
    protected double resolution  = 0;
    /**
     * If true, then the input value "wraps around" such that minval = maxval
     * For a periodic value, the input must be strictly less than maxval,
     * otherwise maxval is a true upper bound.
     */
    protected boolean periodic = true;
    /** The minimum value of the input signal.  */
    protected double minVal = 0;
    /** The maximum value of the input signal. */
    protected double maxVal = 0;
    /** if true, non-periodic inputs smaller than minval or greater
            than maxval will be clipped to minval/maxval */
    protected boolean clipInput;
    /** if true, skip some safety checks (for compatibility reasons), default false */
    protected boolean forced;
    /** Encoder name - an optional string which will become part of the description */
    protected String name = "";
    protected int padding;
    protected int nInternal;
    protected double rangeInternal;
    protected double range;
    protected boolean encLearningEnabled;
    protected Set<FieldMetaType> flattenedFieldTypeList;
    protected Map<Tuple, List<FieldMetaType>> decoderFieldTypes;
    /**
     * This matrix is used for the topDownCompute. We build it the first time
     * topDownCompute is called
     */
    protected SparseObjectMatrix<int[]> topDownMapping;
    protected double[] topDownValues;
    protected List<?> bucketValues;
    protected LinkedHashMap<EncoderTuple, List<EncoderTuple>> encoders;
    protected List<String> scalarNames;


    protected Encoder() {}

    ///////////////////////////////////////////////////////////
    /**
     * Sets the "w" or width of the output signal
     * <em>Restriction:</em> w must be odd to avoid centering problems.
     * @param w
     */
    public void setW(int w) {
    	this.w = w;
    }

    /**
     * Returns w
     * @return
     */
    public int getW() {
    	return w;
    }

    /**
     * Half the width
     * @param hw
     */
    public void setHalfWidth(int hw) {
    	this.halfWidth = hw;
    }

    /**
     * For non-periodic inputs, padding is the number of bits "outside" the range,
     * on each side. I.e. the representation of minval is centered on some bit, and
     * there are "padding" bits to the left of that centered bit; similarly with
     * bits to the right of the center bit of maxval
     *
     * @param padding
     */
    public void setPadding(int padding) {
    	this.padding = padding;
    }

    /**
     * For non-periodic inputs, padding is the number of bits "outside" the range,
     * on each side. I.e. the representation of minval is centered on some bit, and
     * there are "padding" bits to the left of that centered bit; similarly with
     * bits to the right of the center bit of maxval
     *
     * @return
     */
    public int getPadding() {
    	return padding;
    }

    /**
     * Sets rangeInternal
     * @param r
     */
    public void setRangeInternal(double r) {
    	this.rangeInternal = r;
    }

    /**
     * Returns the range internal value
     * @return
     */
    public double getRangeInternal() {
    	return rangeInternal;
    }

    /**
     * Sets the range
     * @param range
     */
    public void setRange(double range) {
    	this.range = range;
    }

    /**
     * Returns the range
     * @return
     */
    public double getRange() {
    	return range;
    }

    /**
     * nInternal represents the output area excluding the possible padding on each side
     *
     * @param n
     */
    public void setNInternal(int n) {
    	this.nInternal = n;
    }

    /**
     * nInternal represents the output area excluding the possible padding on each
     * side
     * @return
     */
    public int getNInternal() {
    	return nInternal;
    }

    /**
     * This matrix is used for the topDownCompute. We build it the first time
     * topDownCompute is called
     *
     * @param sm
     */
    public void setTopDownMapping(SparseObjectMatrix<int[]> sm) {
    	this.topDownMapping = sm;
    }

    /**
     * Range of values.
     * @param values
     */
    public void setTopDownValues(double[] values) {
    	this.topDownValues = values;
    }

    /**
     * Returns the top down range of values
     * @return
     */
    public double[] getTopDownValues() {
    	return topDownValues;
    }

    /**
     * Return the half width value.
     * @return
     */
    public int getHalfWidth() {
    	return halfWidth;
    }

    /**
     * The number of bits in the output. Must be greater than or equal to w
     * @param n
     */
    public void setN(int n) {
    	this.n = n;
    }

    /**
     * Returns n
     * @return
     */
    public int getN() {
    	return n;
    }

    /**
     * The minimum value of the input signal.
     * @param minVal
     */
    public void setMinVal(double minVal) {
    	this.minVal = minVal;
    }

    /**
     * Returns minval
     * @return
     */
    public double getMinVal() {
    	return minVal;
    }

    /**
     * The maximum value of the input signal.
     * @param maxVal
     */
    public void setMaxVal(double maxVal) {
    	this.maxVal = maxVal;
    }

    /**
     * Returns maxval
     * @return
     */
    public double getMaxVal() {
    	return maxVal;
    }

    /**
     * inputs separated by more than, or equal to this distance will have non-overlapping
     * representations
     *
     * @param radius
     */
    public void setRadius(double radius) {
    	this.radius = radius;
    }

    /**
     * Returns the radius
     * @return
     */
    public double getRadius() {
    	return radius;
    }

    /**
     * inputs separated by more than, or equal to this distance will have different
     * representations
     *
     * @param resolution
     */
    public void setResolution(double resolution) {
    	this.resolution = resolution;
    }

    /**
     * Returns the resolution
     * @return
     */
    public double getResolution() {
    	return resolution;
    }

    /**
     * If true, non-periodic inputs smaller than minval or greater
     * than maxval will be clipped to minval/maxval
     * @param b
     */
    public void setClipInput(boolean b) {
    	this.clipInput = b;
    }

    /**
     * Returns the clip input flag
     * @return
     */
    public boolean clipInput() {
    	return clipInput;
    }

    /**
     * If true, then the input value "wraps around" such that minval = maxval
     * For a periodic value, the input must be strictly less than maxval,
     * otherwise maxval is a true upper bound.
     *
     * @param b
     */
    public void setPeriodic(boolean b) {
    	this.periodic = b;
    }

    /**
     * Returns the periodic flag
     * @return
     */
    public boolean isPeriodic() {
    	return periodic;
    }

    /**
     * If true, skip some safety checks (for compatibility reasons), default false
     * @param b
     */
    public void setForced(boolean b) {
    	this.forced = b;
    }

    /**
     * Returns the forced flag
     * @return
     */
    public boolean isForced() {
    	return forced;
    }

    /**
     * An optional string which will become part of the description
     * @param name
     */
    public void setName(String name) {
    	this.name = name;
    }

    /**
     * Returns the optional name
     * @return
     */
    public String getName() {
    	return name;
    }

    /**
     * Adds a the specified {@link Encoder} to the list of the specified
     * parent's {@code Encoder}s.
     *
     * @param parent	the parent Encoder
     * @param name		Name of the {@link Encoder}
     * @param e			the {@code Encoder}
     * @param offset	the offset of the encoded output the specified encoder
     * 					was used to encode.
     */
    public void addEncoder(Encoder<T> parent, String name, Encoder<T> child, int offset) {
    	if(encoders == null) {
    		encoders = new LinkedHashMap<EncoderTuple, List<EncoderTuple>>();
    	}

    	EncoderTuple key = getEncoderTuple(parent);
    	// Insert a new Tuple for the parent if not yet added.
    	if(key == null) {
    	    encoders.put(key = new EncoderTuple("", this, 0), new ArrayList<EncoderTuple>());
    	}
    	
    	List<EncoderTuple> childEncoders = null;
    	if((childEncoders = encoders.get(key)) == null) {
    		encoders.put(key, childEncoders = new ArrayList<EncoderTuple>());
    	}
    	childEncoders.add(new EncoderTuple(name, child, offset));
    }

    /**
     * Returns the {@link Tuple} containing the specified {@link Encoder}
     * @param e		the Encoder the return value should contain
     * @return		the {@link Tuple} containing the specified {@link Encoder}
     */
    public EncoderTuple getEncoderTuple(Encoder<T> e) {
    	if(encoders == null) {
    		encoders = new LinkedHashMap<EncoderTuple, List<EncoderTuple>>();
    	}

    	for(EncoderTuple tuple : encoders.keySet()) {
    		if(tuple.getEncoder().equals(e)) {
    			return tuple;
    		}
    	}
    	return null;
    }

    /**
     * Returns the list of child {@link Encoder} {@link Tuple}s
     * corresponding to the specified {@code Encoder}
     *
     * @param e		the parent {@link Encoder} whose child Encoder Tuples are being returned
     * @return		the list of child {@link Encoder} {@link Tuple}s
     */
    public List<EncoderTuple> getEncoders(Encoder<T> e) {
    	return getEncoders().get(getEncoderTuple(e));
    }

    /**
     * Returns the list of {@link Encoder}s
     * @return
     */
    public Map<EncoderTuple, List<EncoderTuple>> getEncoders() {
    	if(encoders == null) {
    		encoders = new LinkedHashMap<EncoderTuple, List<EncoderTuple>>();
    	}
    	return encoders;
    }

    /**
     * Sets the encoder flag indicating whether learning is enabled.
     *
     * @param	encLearningEnabled	true if learning is enabled, false if not
     */
    public void setLearningEnabled(boolean encLearningEnabled) {
    	this.encLearningEnabled = encLearningEnabled;
    }

    /**
     * Returns a flag indicating whether encoder learning is enabled.
     */
    public boolean isEncoderLearningEnabled() {
    	return encLearningEnabled;
    }

    /**
     * Returns the list of all field types of the specified {@link Encoder}.
     *
     * @return	List<FieldMetaType>
     */
    public List<FieldMetaType> getFlattenedFieldTypeList(Encoder<T> e) {
    	if(decoderFieldTypes == null) {
    		decoderFieldTypes = new HashMap<Tuple, List<FieldMetaType>>();
    	}

    	Tuple key = getEncoderTuple(e);
    	List<FieldMetaType> fieldTypes = null;
    	if((fieldTypes = decoderFieldTypes.get(key)) == null) {
    		decoderFieldTypes.put(key, fieldTypes = new ArrayList<FieldMetaType>());
    	}
    	return fieldTypes;
    }

    /**
     * Returns the list of all field types of a parent {@link Encoder} and all
     * leaf encoders flattened in a linear list which does not retain any parent
     * child relationship information.
     *
     * @return	List<FieldMetaType>
     */
    public Set<FieldMetaType> getFlattenedFieldTypeList() {
    	return flattenedFieldTypeList;
    }

    /**
     * Sets the list of flattened {@link FieldMetaType}s
     *
     * @param l		list of {@link FieldMetaType}s
     */
    public void setFlattenedFieldTypeList(Set<FieldMetaType> l) {
    	this.flattenedFieldTypeList = l;
    }

    /**
     * Returns the names of the fields
     *
     * @return	the list of names
     */
    public List<String> getScalarNames() {
    	return scalarNames;
    }

    /**
     * Sets the names of the fields
     *
     * @param names	the list of names
     */
    public void setScalarNames(List<String> names) {
    	this.scalarNames = names;
    }
    ///////////////////////////////////////////////////////////


	/**
	 * Should return the output width, in bits.
	 */
	public abstract int getWidth();

	/**
	 * Returns true if the underlying encoder works on deltas
	 */
	public abstract boolean isDelta();

	/**
	 * Encodes inputData and puts the encoded value into the output array,
     * which is a 1-D array of length returned by {@link #getW()}.
	 *
     * Note: The output array is reused, so clear it before updating it.
	 * @param inputData Data to encode. This should be validated by the encoder.
	 * @param output 1-D array of same length returned by {@link #getW()}
     *
	 * @return
	 */
	public abstract void encodeIntoArray(T inputData, int[] output);

	/**
	 * Set whether learning is enabled.
	 * @param 	learningEnabled		flag indicating whether learning is enabled
	 */
    public void setLearning(boolean learningEnabled) {
        setLearningEnabled(learningEnabled);
    }

	/**
	 * This method is called by the model to set the statistics like min and
     * max for the underlying encoders if this information is available.
	 * @param	fieldName			fieldName name of the field this encoder is encoding, provided by
     *     							{@link MultiEncoder}
	 * @param	fieldStatistics		fieldStatistics dictionary of dictionaries with the first level being
     *     							the fieldName and the second index the statistic ie:
     *     							fieldStatistics['pounds']['min']
	 */
	public void setFieldStats(String fieldName, Map<String, Double> fieldStatistics) {}

	/**
	 * Convenience wrapper for {@link #encodeIntoArray(double, int[])}
	 * @param inputData		the input scalar
	 *
     * @return	an array with the encoded representation of inputData
	 */
	public int[] encode(T inputData) {
		int[] output = new int[getN()];
		encodeIntoArray(inputData, output);
		return output;
	}

	/**
	 * Return the field names for each of the scalar values returned by
     * .
	 * @param parentFieldName	parentFieldName The name of the encoder which is our parent. This name
     *     						is prefixed to each of the field names within this encoder to form the
     *      					keys of the dict() in the retval.
     *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<String> getScalarNames(String parentFieldName) {
		List<String> names = new ArrayList<String>();
		if(getEncoders() != null) {
			List<EncoderTuple> encoders = getEncoders(this);
			for(Tuple tuple : encoders) {
				List<String> subNames = ((Encoder<T>)tuple.get(1)).getScalarNames(getName());
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
				names.add((String)getEncoderTuple(this).get(0));
			}
		}

		return names;
	}

	/**
	 * Returns a sequence of field types corresponding to the elements in the
     * decoded output field array.  The types are defined by {@link FieldMetaType}
     *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Set<FieldMetaType> getDecoderOutputFieldTypes() {
		if(getFlattenedFieldTypeList() != null) {
			return new HashSet<>(getFlattenedFieldTypeList());
		}

		Set<FieldMetaType> retVal = new HashSet<FieldMetaType>();
		for(Tuple t : getEncoders(this)) {
			Set<FieldMetaType> subTypes = ((Encoder<T>)t.get(1)).getDecoderOutputFieldTypes();
			retVal.addAll(subTypes);
		}
		setFlattenedFieldTypeList(retVal);
		return retVal;
	}

	/**
	 * Gets the value of a given field from the input record
	 * @param inputObject	input object
	 * @param fieldName		the name of the field containing the input object.
	 * @return
	 */
	public Object getInputValue(Object inputObject, String fieldName) {
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
	 * Returns an {@link TDoubleList} containing the sub-field scalar value(s) for
     * each sub-field of the inputData. To get the associated field names for each of
     * the scalar values, call getScalarNames().
	 *
     * For a simple scalar encoder, the scalar value is simply the input unmodified.
     * For category encoders, it is the scalar representing the category string
     * that is passed in.
     *
     * TODO This is not correct for DateEncoder:
     *
     * For the datetime encoder, the scalar value is the
     * the number of seconds since epoch.
	 *
     * The intent of the scalar representation of a sub-field is to provide a
     * baseline for measuring error differences. You can compare the scalar value
     * of the inputData with the scalar value returned from topDownCompute() on a
     * top-down representation to evaluate prediction accuracy, for example.
     *
     * @param <S>  the specifically typed input object
     *
	 * @return
	 */
	public <S> TDoubleList getScalars(S d) {
		TDoubleList retVals = new TDoubleArrayList();
		double inputData = (Double)d;
		List<EncoderTuple> encoders = getEncoders(this);
		if(encoders != null) {
			for(EncoderTuple t : encoders) {
				TDoubleList values = t.getEncoder().getScalars(inputData);
				retVals.addAll(values);
			}
		}
		return retVals;
	}

	/**
	 * Returns the input in the same format as is returned by topDownCompute().
     * For most encoder types, this is the same as the input data.
     * For instance, for scalar and category types, this corresponds to the numeric
     * and string values, respectively, from the inputs. For datetime encoders, this
     * returns the list of scalars for each of the sub-fields (timeOfDay, dayOfWeek, etc.)
	 *
     * This method is essentially the same as getScalars() except that it returns
     * strings
	 * @param <S> 	The input data in the format it is received from the data source
	 *
     * @return A list of values, in the same format and in the same order as they
     * are returned by topDownCompute.
     *
	 * @return	list of encoded values in String form
	 */
	public <S> List<String> getEncodedValues(S inputData) {
		List<String> retVals = new ArrayList<String>();
		Map<EncoderTuple, List<EncoderTuple>> encoders = getEncoders();
		if(encoders != null && encoders.size() > 0) {
			for(EncoderTuple t : encoders.keySet()) {
				retVals.addAll(t.getEncoder().getEncodedValues(inputData));
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
	 * @param  	input 	The data from the source. This is typically a object with members.
	 *
	 * @return 	array of bucket indices
	 */
	public int[] getBucketIndices(String input) {
		TIntList l = new TIntArrayList();
		Map<EncoderTuple, List<EncoderTuple>> encoders = getEncoders();
		if(encoders != null && encoders.size() > 0) {
			for(EncoderTuple t : encoders.keySet()) {
				l.addAll(t.getEncoder().getBucketIndices(input));
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
	 * @param  	input 	The data from the source. This is typically a object with members.
	 *
	 * @return 	array of bucket indices
	 */
	public int[] getBucketIndices(double input) {
		TIntList l = new TIntArrayList();
		Map<EncoderTuple, List<EncoderTuple>> encoders = getEncoders();
		if(encoders != null && encoders.size() > 0) {
			for(EncoderTuple t : encoders.keySet()) {
				l.addAll(t.getEncoder().getBucketIndices(input));
			}
		}else{
			throw new IllegalStateException("Should be implemented in base classes that are not " +
				"containers for other encoders");
		}
		return l.toArray();
	}

	/**
	 * Return a pretty print string representing the return values from
     * getScalars and getScalarNames().
	 * @param scalarValues 	input values to encode to string
	 * @param scalarNames 	optional input of scalar names to convert. If None, gets
     *                  	scalar names from getScalarNames()
	 *
	 * @return string representation of scalar values
	 */
	public String scalarsToStr(List<?> scalarValues, List<String> scalarNames) {
		if(scalarNames == null || scalarNames.isEmpty()) {
			scalarNames = getScalarNames("");
		}

		StringBuilder desc = new StringBuilder();
		for(Tuple t : ArrayUtils.zip(scalarNames, scalarValues)) {
			if(desc.length() > 0) {
				desc.append(String.format(", %s:%.2f", t.get(0), t.get(1)));
			}else{
				desc.append(String.format("%s:%.2f", t.get(0), t.get(1)));
			}
		}
		return desc.toString();
	}

	/**
	 * This returns a list of tuples, each containing (name, offset).
     * The 'name' is a string description of each sub-field, and offset is the bit
     * offset of the sub-field for that encoder.
	 *
     * For now, only the 'multi' and 'date' encoders have multiple (name, offset)
     * pairs. All other encoders have a single pair, where the offset is 0.
     *
	 * @return		list of tuples, each containing (name, offset)
	 */
     public List<Tuple> getDescription() {
           return description;
     }


	/**
	 * Return a description of the given bit in the encoded output.
     * This will include the field name and the offset within the field.
	 * @param bitOffset  	Offset of the bit to get the description of
	 * @param formatted     If True, the bitOffset is w.r.t. formatted output,
     *                     	which includes separators
	 *
     * @return tuple(fieldName, offsetWithinField)
	 */
	public Tuple encodedBitDescription(int bitOffset, boolean formatted) {
		//Find which field it's in
		List<Tuple> description = getDescription();
		int len = description.size();
		String prevFieldName = null;
		int prevFieldOffset = -1;
		int offset = -1;
		for(int i = 0;i < len;i++) {
			Tuple t = description.get(i);//(name, offset)
			if(formatted) {
				offset = ((int)t.get(1)) + 1;
				if(bitOffset == offset - 1) {
					prevFieldName = "separator";
					prevFieldOffset = bitOffset;
				}
			}
			if(bitOffset < offset) break;
		}
		// Return the field name and offset within the field
	    // return (fieldName, bitOffset - fieldOffset)
		int width = formatted ? getDisplayWidth() : getWidth();

		if(prevFieldOffset == -1 || bitOffset > getWidth()) {
			throw new IllegalStateException("Bit is outside of allowable range: " +
				String.format("[0 - %d]", width));
		}
		return new Tuple(prevFieldName, bitOffset - prevFieldOffset);
	}

	/**
	 * Pretty-print a header that labels the sub-fields of the encoded
     * output. This can be used in conjunction with {@link #pprint(int[], String)}.
	 * @param prefix
	 */
	public void pprintHeader(String prefix) {
		LOGGER.info(prefix == null ? "" : prefix);

		List<Tuple> description = getDescription();
		description.add(new Tuple("end", getWidth()));

		int len = description.size() - 1;
		for(int i = 0;i < len;i++) {
			String name = (String)description.get(i).get(0);
			int width = (int)description.get(i+1).get(1);

			String formatStr = String.format("%%-%ds |", width);
			StringBuilder pname = new StringBuilder(name);
			if(name.length() > width) pname.setLength(width);

            LOGGER.info(String.format(formatStr, pname));
		}

		len = getWidth() + (description.size() - 1)*3 - 1;
		StringBuilder hyphens = new StringBuilder();
		for(int i = 0;i < len;i++) hyphens.append("-");
        LOGGER.info(new StringBuilder(prefix).append(hyphens).toString());
    }

    /**
	 * Pretty-print the encoded output using ascii art.
	 * @param output
	 * @param prefix
	 */
	public void pprint(int[] output, String prefix) {
		LOGGER.info(prefix == null ? "" : prefix);

		List<Tuple> description = getDescription();
		description.add(new Tuple("end", getWidth()));

		int len = description.size() - 1;
		for(int i = 0;i < len;i++) {
			int offset = (int)description.get(i).get(1);
			int nextOffset = (int)description.get(i + 1).get(1);

            LOGGER.info(
                    String.format("%s |",
                            ArrayUtils.bitsToString(
                                    ArrayUtils.sub(output, ArrayUtils.range(offset, nextOffset))
                            )
                    )
            );
        }
    }

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
	 * @param encoded      		The encoded output that you want decode
	 * @param parentFieldName 	The name of the encoder which is our parent. This name
     *      					is prefixed to each of the field names within this encoder to form the
     *    						keys of the {@link Map} returned.
     *
	 * @returns Tuple(fieldsMap, fieldOrder)
	 */
	@SuppressWarnings("unchecked")
	public Tuple decode(int[] encoded, String parentFieldName) {
		Map<String, Tuple> fieldsMap = new HashMap<String, Tuple>();
		List<String> fieldsOrder = new ArrayList<String>();

		String parentName = parentFieldName == null || parentFieldName.isEmpty() ?
			getName() : String.format("%s.%s", parentFieldName, getName());

		List<EncoderTuple> encoders = getEncoders(this);
		int len = encoders.size();
		for(int i = 0;i < len;i++) {
			Tuple threeFieldsTuple = encoders.get(i);
			int nextOffset = 0;
			if(i < len - 1) {
				nextOffset = (Integer)encoders.get(i + 1).get(2);
			}else{
				nextOffset = getW();
			}

			int[] fieldOutput = ArrayUtils.sub(encoded, ArrayUtils.range((Integer)threeFieldsTuple.get(2), nextOffset));

			Tuple result = ((Encoder<T>)threeFieldsTuple.get(1)).decode(fieldOutput, parentName);

			fieldsMap.putAll((Map<String, Tuple>)result.get(0));
			fieldsOrder.addAll((List<String>)result.get(1));
		}

		return new Tuple(fieldsMap, fieldsOrder);
	}

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
     * @return  list of items, each item representing the bucket value for that
     *          bucket.
	 */
	public abstract <S> List<S> getBucketValues(Class<S> returnType);

	/**
	 * Returns a list of {@link Encoding}s describing the inputs for
     * each sub-field that correspond to the bucket indices passed in 'buckets'.
     * To get the associated field names for each of the values, call getScalarNames().
	 * @param buckets 	The list of bucket indices, one for each sub-field encoder.
     *              	These bucket indices for example may have been retrieved
     *              	from the getBucketIndices() call.
	 *
     * @return A list of {@link Encoding}s. Each EncoderResult has
	 */
	@SuppressWarnings("unchecked")
	public List<Encoding> getBucketInfo(int[] buckets) {
		//Concatenate the results from bucketInfo on each child encoder
		List<Encoding> retVals = new ArrayList<Encoding>();
		int bucketOffset = 0;
		for(EncoderTuple encoderTuple : getEncoders(this)) {
			int nextBucketOffset = -1;
			List<EncoderTuple> childEncoders = null;
			if((childEncoders = getEncoders((Encoder<T>)encoderTuple.getEncoder())) != null) {
				nextBucketOffset = bucketOffset + childEncoders.size();
			}else{
				nextBucketOffset = bucketOffset + 1;
			}
			int[] bucketIndices = ArrayUtils.sub(buckets, ArrayUtils.range(bucketOffset, nextBucketOffset));
			List<Encoding> values = encoderTuple.getEncoder().getBucketInfo(bucketIndices);

			retVals.addAll(values);

			bucketOffset = nextBucketOffset;
		}

		return retVals;
	}

	/**
	 * Returns a list of EncoderResult named tuples describing the top-down
     * best guess inputs for each sub-field given the encoded output. These are the
     * values which are most likely to generate the given encoded output.
     * To get the associated field names for each of the values, call
     * getScalarNames().
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
     *        -# encoding       This is the encoded bit-array
     *                          that represents the best-guess value.
     *                          That is, if 'value' was passed to
     *                          encode(), an identical bit-array should be
     *                          returned.
	 */
	@SuppressWarnings("unchecked")
	public List<Encoding> topDownCompute(int[] encoded) {
		List<Encoding> retVals = new ArrayList<Encoding>();

		List<EncoderTuple> encoders = getEncoders(this);
		int len = encoders.size();
		for(int i = 0;i < len;i++) {
			int offset = (int)encoders.get(i).get(2);
			Encoder<T> encoder = (Encoder<T>)encoders.get(i).get(1);

			int nextOffset;
			if(i < len - 1) {
				//Encoders = List<Encoder> : Encoder = EncoderTuple(name, encoder, offset)
				nextOffset = (int)encoders.get(i + 1).get(2);
			}else{
				nextOffset = getW();
			}

			int[] fieldOutput = ArrayUtils.sub(encoded, ArrayUtils.range(offset, nextOffset));
			List<Encoding> values = encoder.topDownCompute(fieldOutput);

			retVals.addAll(values);
		}

		return retVals;
	}

	public TDoubleList closenessScores(TDoubleList expValues, TDoubleList actValues, boolean fractional) {
		TDoubleList retVal = new TDoubleArrayList();

		//Fallback closenss is a percentage match
		List<EncoderTuple> encoders = getEncoders(this);
		if(encoders == null || encoders.size() < 1) {
			double err = Math.abs(expValues.get(0) - actValues.get(0));
			double closeness = -1;
			if(fractional) {
				double denom = Math.max(expValues.get(0), actValues.get(0));
				if(denom == 0) {
					denom = 1.0;
				}

				closeness = 1.0 - err/denom;
				if(closeness < 0) {
					closeness = 0;
				}
			}else{
				closeness = err;
			}

			retVal.add(closeness);
			return retVal;
		}

		int scalarIdx = 0;
		for(EncoderTuple res : getEncoders(this)) {
			TDoubleList values = res.getEncoder().closenessScores(
				expValues.subList(scalarIdx, expValues.size()), actValues.subList(scalarIdx, actValues.size()), fractional);

			scalarIdx += values.size();
			retVal.addAll(values);
		}

		return retVal;
	}

	/**
     * Returns an array containing the sum of the right
     * applied multiplications of each slice to the array
     * passed in.
     *
     * @param encoded
     * @return
     */
    public int[] rightVecProd(SparseObjectMatrix<int[]> matrix, int[] encoded) {
    	int[] retVal = new int[matrix.getMaxIndex() + 1];
    	for(int i = 0;i < retVal.length;i++) {
    		int[] slice = matrix.getObject(i);
    		for(int j = 0;j < slice.length;j++) {
    			retVal[i] += (slice[j] * encoded[j]);
    		}
    	}
    	return retVal;
    }

	/**
	 * Calculate width of display for bits plus blanks between fields.
	 *
	 * @return	width
	 */
	public int getDisplayWidth() {
		return getWidth() + getDescription().size() - 1;
	}

	/**
	 * Base class for {@link Encoder} builders
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public static abstract class Builder<K, E> {
		protected int n;
		protected int w;
		protected double minVal;
		protected double maxVal;
		protected double radius;
		protected double resolution;
		protected boolean periodic;
		protected boolean clipInput;
		protected boolean forced;
		protected String name;

		protected Encoder<?> encoder;

		public E build() {
			if(encoder == null) {
				throw new IllegalStateException("Subclass did not instantiate builder type " +
					"before calling this method!");
			}
			encoder.setN(n);
			encoder.setW(w);
			encoder.setMinVal(minVal);
			encoder.setMaxVal(maxVal);
			encoder.setRadius(radius);
			encoder.setResolution(resolution);
			encoder.setPeriodic(periodic);
			encoder.setClipInput(clipInput);
			encoder.setForced(forced);
			encoder.setName(name);

			return (E)encoder;
		}

		public K n(int n) {
			this.n = n;
			return (K)this;
		}
		public K w(int w) {
			this.w = w;
			return (K)this;
		}
		public K minVal(double minVal) {
			this.minVal = minVal;
			return (K)this;
		}
		public K maxVal(double maxVal) {
			this.maxVal = maxVal;
			return (K)this;
		}
		public K radius(double radius) {
			this.radius = radius;
			return (K)this;
		}
		public K resolution(double resolution) {
			this.resolution = resolution;
			return (K)this;
		}
		public K periodic(boolean periodic) {
			this.periodic = periodic;
			return (K)this;
		}
		public K clipInput(boolean clipInput) {
			this.clipInput = clipInput;
			return (K)this;
		}
		public K forced(boolean forced) {
			this.forced = forced;
			return (K)this;
		}
		public K name(String name) {
			this.name = name;
			return (K)this;
		}
	}
}
