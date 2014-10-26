package org.numenta.nupic.encoders;

import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.Tuple;

/**
 * Subclass of Tuple to specifically contain the results of an
 * {@link Encoder}'s {@link Encoder#encode(org.numenta.nupic.research.Connections, double)}
 * call.
 * 
 * @author David Ray
 *
 * @param <T>	the fieldsMap
 * @param <K>	the fieldsOrder
 */
public class DecodeTuple<M extends Map<String, Ranges>, L extends List<String>> extends Tuple {
	protected M fields;
	protected L fieldDescriptions;
	
	public DecodeTuple(M m, L l) {
		super(2, m, l);
		this.fields = m;
		this.fieldDescriptions = l;
	}
}
