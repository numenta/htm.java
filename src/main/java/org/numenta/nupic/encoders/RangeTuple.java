package org.numenta.nupic.encoders;

import java.util.List;

import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

/**
 * Subclasses the {@link Tuple} utility class to constrain 
 * the number of arguments and argument types to those specifically
 * related to the {@link Encoder} functionality.
 * 
 * @author David Ray
 *
 * @param <L>
 * @param <S>
 */
public class RangeTuple<L extends List<MinMax>, S> extends Tuple {
	protected L l;
	protected String desc;
	
	/**
	 * Instantiates a {@code RangeTuple}
	 * @param l
	 * @param s
	 */
	public RangeTuple(L l, String s) {
		super(2, l, s);
		this.l = l;
		this.desc = s;
	}
}
