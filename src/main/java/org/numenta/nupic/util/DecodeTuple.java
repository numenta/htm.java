package org.numenta.nupic.util;

import java.util.List;
import java.util.Map;

import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.RangeList;

/**
 * Subclass of Tuple to specifically contain the results of an
 * {@link Encoder}'s {@link Encoder#encode(double)}
 * call.
 * 
 * @author David Ray
 *
 * @param <T>	the fieldsMap
 * @param <K>	the fieldsOrder
 */
public class DecodeTuple<M extends Map<String, RangeList>, L extends List<String>> extends Tuple {
	protected M fields;
	protected L fieldDescriptions;
	
	public DecodeTuple(M m, L l) {
		super(2, m, l);
		this.fields = m;
		this.fieldDescriptions = l;
	}
}
