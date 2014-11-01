package org.numenta.nupic.encoders;


public class CategoryEncoder extends ScalarEncoder {
	public void init() {
		if(getW() % 2 == 0) {
			throw new IllegalStateException(
				"W must be an odd number (to eliminate centering difficulty)");
		}
		
		setHalfWidth((getW() - 1) / 2);
		
		// For non-periodic inputs, padding is the number of bits "outside" the range,
	    // on each side. I.e. the representation of minval is centered on some bit, and
	    // there are "padding" bits to the left of that centered bit; similarly with
	    // bits to the right of the center bit of maxval
		setPadding(isPeriodic() ? 0 : getHalfWidth());
		
		if(getMinVal() != 0 && getMaxVal() != 0) {
			if(getMinVal() >= getMaxVal()) {
				throw new IllegalStateException("maxVal must be > minVal");
			}
			setRangeInternal(getMaxVal() - getMinVal());
		}
		
		// There are three different ways of thinking about the representation. Handle
	    // each case here.
//		initEncoder(c, getW(), getMinVal(), getMaxVal(), getN(), getRadius(), getResolution());
		
		//nInternal represents the output area excluding the possible padding on each side
		setNInternal(getN() - 2 * getPadding());
		
		if(getName() == null) {
			if((getMinVal() % ((int)getMinVal())) > 0 ||
			    (getMaxVal() % ((int)getMaxVal())) > 0) {
				setName("[" + getMinVal() + ":" + getMaxVal() + "]");
			}else{
				setName("[" + (int)getMinVal() + ":" + (int)getMaxVal() + "]");
			}
		}
		
		//Checks for likely mistakes in encoder settings
		if(!isForced()) {
//			checkReasonableSettings(c);
		}
	}

}
