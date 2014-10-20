package org.numenta.nupic.encoders;

import org.numenta.nupic.research.Connections;


/**
 * DOCUMENTATION TAKE DIRECTLY FROM THE PYTHON VERSION:
 * 
 * A scalar encoder encodes a numeric (floating point) value into an array
 * of bits. The output is 0's except for a contiguous block of 1's. The
 * location of this contiguous block varies continuously with the input value.
 *
 * The encoding is linear. If you want a nonlinear encoding, just transform
 * the scalar (e.g. by applying a logarithm function) before encoding.
 * It is not recommended to bin the data as a pre-processing step, e.g.
 * "1" = $0 - $.20, "2" = $.21-$0.80, "3" = $.81-$1.20, etc. as this
 * removes a lot of information and prevents nearby values from overlapping
 * in the output. Instead, use a continuous transformation that scales
 * the data (a piecewise transformation is fine).
 *
 *
 * Parameters:
 * -----------------------------------------------------------------------------
 * w --        The number of bits that are set to encode a single value - the
 *             "width" of the output signal
 *             restriction: w must be odd to avoid centering problems.
 *
 * minval --   The minimum value of the input signal.
 *
 * maxval --   The upper bound of the input signal
 *
 * periodic -- If true, then the input value "wraps around" such that minval = maxval
 *             For a periodic value, the input must be strictly less than maxval,
 *             otherwise maxval is a true upper bound.
 *
 * There are three mutually exclusive parameters that determine the overall size of
 * of the output. Only one of these should be specifed to the constructor:
 *
 * n      --      The number of bits in the output. Must be greater than or equal to w
 * radius --      Two inputs separated by more than the radius have non-overlapping
 *                representations. Two inputs separated by less than the radius will
 *                in general overlap in at least some of their bits. You can think
 *                of this as the radius of the input.
 * resolution --  Two inputs separated by greater than, or equal to the resolution are guaranteed
 *                 to have different representations.
 *
 * Note: radius and resolution are specified w.r.t the input, not output. w is
 * specified w.r.t. the output.
 *
 * Example:
 * day of week.
 * w = 3
 * Minval = 1 (Monday)
 * Maxval = 8 (Monday)
 * periodic = true
 * n = 14
 * [equivalently: radius = 1.5 or resolution = 0.5]
 *
 * The following values would encode midnight -- the start of the day
 * monday (1)   -> 11000000000001
 * tuesday(2)   -> 01110000000000
 * wednesday(3) -> 00011100000000
 * ...
 * sunday (7)   -> 10000000000011
 *
 * Since the resolution is 12 hours, we can also encode noon, as
 * monday noon  -> 11100000000000
 * monday midnt-> 01110000000000
 * tuesday noon -> 00111000000000
 * etc.
 *
 *
 * It may not be natural to specify "n", especially with non-periodic
 * data. For example, consider encoding an input with a range of 1-10
 * (inclusive) using an output width of 5.  If you specify resolution =
 * 1, this means that inputs of 1 and 2 have different outputs, though
 * they overlap, but 1 and 1.5 might not have different outputs.
 * This leads to a 14-bit representation like this:
 *
 * 1 ->  11111000000000  (14 bits total)
 * 2 ->  01111100000000
 * ...
 * 10->  00000000011111
 * [resolution = 1; n=14; radius = 5]
 *
 * You could specify resolution = 0.5, which gives
 * 1   -> 11111000... (22 bits total)
 * 1.5 -> 011111.....
 * 2.0 -> 0011111....
 * [resolution = 0.5; n=22; radius=2.5]
 *
 * You could specify radius = 1, which gives
 * 1   -> 111110000000....  (50 bits total)
 * 2   -> 000001111100....
 * 3   -> 000000000011111...
 * ...
 * 10  ->                           .....000011111
 * [radius = 1; resolution = 0.2; n=50]
 *
 *
 * An N/M encoding can also be used to encode a binary value,
 * where we want more than one bit to represent each state.
 * For example, we could have: w = 5, minval = 0, maxval = 1,
 * radius = 1 (which is equivalent to n=10)
 * 0 -> 1111100000
 * 1 -> 0000011111
 *
 *
 * Implementation details:
 * --------------------------------------------------------------------------
 * range = maxval - minval
 * h = (w-1)/2  (half-width)
 * resolution = radius / w
 * n = w * range/radius (periodic)
 * n = w * range/radius + 2 * h (non-periodic)
 * 
 * @author metaware
 */
public class ScalarEncoder extends Encoder {
	/**
	 * Constructs a new {@code ScalarEncoder}
	 */
	public ScalarEncoder() {}
	
	/**
	 * w -- number of bits to set in output
     * minval -- minimum input value
     * maxval -- maximum input value (input is strictly less if periodic == True)
	 *
     * Exactly one of n, radius, resolution must be set. "0" is a special
     * value that means "not set".
	 *
     * n -- number of bits in the representation (must be > w)
     * radius -- inputs separated by more than, or equal to this distance will have non-overlapping
     * representations
     * resolution -- inputs separated by more than, or equal to this distance will have different
     * representations
	 * 
     * name -- an optional string which will become part of the description
	 *
     * clipInput -- if true, non-periodic inputs smaller than minval or greater
     * than maxval will be clipped to minval/maxval
	 *
     * forced -- if true, skip some safety checks (for compatibility reasons), default false
     * 
	 * @param c		the memory
	 */
	public void init(Connections c) {
		if(c.getW() % 2 == 0) {
			throw new IllegalStateException(
				"W must be an odd number (to eliminate centering difficulty)");
		}
		
		c.setHalfWidth((c.getW() - 1) / 2);
		
		// For non-periodic inputs, padding is the number of bits "outside" the range,
	    // on each side. I.e. the representation of minval is centered on some bit, and
	    // there are "padding" bits to the left of that centered bit; similarly with
	    // bits to the right of the center bit of maxval
		c.setPadding(c.isPeriodic() ? 0 : c.getHalfWidth());
		
		if(c.getMinVal() != 0 && c.getMaxVal() != 0) {
			if(c.getMinVal() >= c.getMaxVal()) {
				throw new IllegalStateException("maxVal must be > minVal");
			}
			c.setRangeInternal(c.getMaxVal() - c.getMinVal());
		}
		
		// There are three different ways of thinking about the representation. Handle
	    // each case here.
		initEncoder(c, c.getMinVal(), c.getW(), c.getMaxVal(), c.getN(), c.getRadius(), c.getResolution());
		
		//nInternal represents the output area excluding the possible padding on each side
		c.setNInternal(c.getN() - 2 * c.getPadding());
		
		if(c.getName() == null) {
			c.setName("[" + c.getMinVal() + ":" + c.getMaxVal() + "]");
		}
		
		//Checks for likely mistakes in encoder settings
		if(!c.isForced()) {
			checkReasonableSettings(c);
		}
	}
	
	/**
	 * There are three different ways of thinking about the representation. 
     * Handle each case here.
     * 
	 * @param c
	 * @param minVal
	 * @param maxVal
	 * @param n
	 * @param radius
	 * @param resolution
	 */
	public void initEncoder(Connections c, int w, int minVal, int maxVal, int n, float radius, float resolution) {
		if(n != 0) {
			if(minVal != 0 && maxVal != 0) {
				c.setResolution(c.getRangeInternal() / (c.getN() - c.getW()));
			}else{
				c.setResolution(c.getRangeInternal() / c.getN());
			}
			
			c.setRadius(c.getW() * c.getResolution());
			
			if(c.isPeriodic()) {
				c.setRange(c.getRangeInternal());
			}else{
				c.setRange(c.getRangeInternal() + c.getResolution());
			}
			
		}else{
			if(radius != 0) {
				c.setResolution(c.getRadius() / w);
			}else if(resolution != 0) {
				c.setRadius(c.getResolution() * w);
			}else{
				throw new IllegalStateException(
					"One of n, radius, resolution must be specified for a ScalarEncoder");
			}
			
			if(c.isPeriodic()) {
				c.setRange(c.getRangeInternal());
			}else{
				c.setRange(c.getRangeInternal() + c.getResolution());
			}
			
			float nFloat = w * (c.getRange() / c.getRadius()) + 2 * c.getPadding();
			c.setN((int)Math.ceil(nFloat));
		}
	}
	
	/**
	 * Check if the settings are reasonable for the SpatialPooler to work
	 * @param c
	 */
	public void checkReasonableSettings(Connections c) {
		if(c.getW() < 21) {
			throw new IllegalStateException(
				"Number of bits in the SDR (%d) must be greater than 2, and recommended >= 21 (use forced=True to override)");
		}
	}

	@Override
	protected int[] encodeIntoArray(Connections c, int[] inputData, int[] output) {
		
		return null;
	}
	
	/**
	 * Return the bit offset of the first bit to be set in the encoder output.
     * For periodic encoders, this can be a negative number when the encoded output
     * wraps around.
     * 
	 * @param c			the memory
	 * @param input		the input data
	 * @return			an encoded array
	 */
	public int[] getFirstOnBit(Connections c, int[] input) {
		if(input == SENTINEL_VALUE_FOR_MISSING_DATA) {
			return SENTINEL_VALUE_FOR_MISSING_DATA;
		}
		return null;
	}
	
}
