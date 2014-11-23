package org.numenta.nupic.encoders;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;
/**
 * 
 * @author wilsondy
 *
 */
public class PassThroughEncoder extends Encoder<int[]> {
	//total #bits in output (must equal input bits * multiply)
	
	/**
	 * used to normalize the sparsity of the output, exactly w bits ON,
     * if Null (default) - do not alter the input, just pass it further.
	 */
	private Integer outputBitsOnCount;

	public PassThroughEncoder(int outputWidth, Integer outputBitsOnCount) {
		super.setW(outputWidth);
		super.setN(outputWidth);
		super.setForced(false);
		this.outputBitsOnCount = outputBitsOnCount;
	}

	@Override
	public gnu.trove.list.TDoubleList closenessScores(gnu.trove.list.TDoubleList expValues, gnu.trove.list.TDoubleList actValues, boolean fractional) {
	TDoubleArrayList result = new TDoubleArrayList();
//	  """Does a bitwise compare of the two bitmaps and returns a fractonal
//    value between 0 and 1 of how similar they are.
//    1 => identical
//    0 => no overlaping bits
//
//    kwargs will have the keyword "fractional", which is assumed by this encoder
//    """

	double ratio = 1.0d;
	double expectedSum = expValues.sum();
	double actualSum = actValues.sum();
	

    if (actualSum > expectedSum) {
      double diff = actualSum - expectedSum;
      if(diff < expectedSum)
        ratio = 1 - diff/expectedSum;
      else
        ratio = 1/diff;
	}

    int[] expectedInts = ArrayUtils.toIntArray(expValues.toArray()); 
    int[] actualInts = ArrayUtils.toIntArray(actValues.toArray());
    
    int[] overlap = ArrayUtils.and(expectedInts, actualInts);
    
    int overlapSum = ArrayUtils.sum(overlap);
//    osum = int(olap.sum())
    double r = 0.0;
    if (expectedSum == 0)
    	r = 0.0;
    else
      r = overlapSum/expectedSum;
    r = r * ratio;
//
//    return numpy.array([r])
	
	result.add(r);
	return result;
	}
	
	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public boolean isDelta() {
		return false;
	}

	

	/**
	 * TODO Why have the output parameter and a return value?
	 * @param input
	 * @param output
	 * @return
	 */
	public void encodeIntoArray(int[] input, int[] output){

		if( input.length != output.length)
			throw new IllegalArgumentException(String.format("Different input (%i) and output (%i) sizes", input.length, output.length));
		if(this.outputBitsOnCount != null && ArrayUtils.sum(input) != outputBitsOnCount)
			throw new IllegalArgumentException(String.format("Input has %i bits but w was set to %i.",  ArrayUtils.sum(input), outputBitsOnCount));
		
		System.arraycopy(input, 0, output, 0, input.length);

	}
	
	public Tuple decode(int[] encoded, String parentFieldName) {
	    //TODO: these methods should be properly implemented (this comment in Python)		  
		String fieldName = this.name;
	    if (parentFieldName != null && parentFieldName.length() >0)
	    	String.format("%s.%s", parentFieldName, this.name);

		List<MinMax> ranges = new ArrayList<MinMax>();
		ranges.add(new MinMax(0,0));
	    RangeList inner = new RangeList(ranges, "input");
		Map<String, RangeList> fieldsDict = new HashMap<String, RangeList>();
		fieldsDict.put(fieldName, inner);
		
	    //return ({fieldName: ([[0, 0]], "input")}, [fieldName])
		return new DecodeResult(fieldsDict, Arrays.asList(new String[] { fieldName }));


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

	@Override
	public <T> List<T> getBucketValues(Class<T> returnType) {
		// TODO Auto-generated method stub
		return null;
	}

}
