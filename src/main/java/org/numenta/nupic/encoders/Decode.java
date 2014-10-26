package org.numenta.nupic.encoders;

import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.MinMax;

/**
 * Tuple to contain the results of an {@link Encoder}'s decoded
 * values.
 * 
 * @author David Ray
 */
public class Decode extends DecodeTuple<Map<String, Ranges>, List<String>> {
	
	/**
	 * Constructs a new {@code Decode}
	 * @param m		Map of field names to {@link Ranges} object
	 * @param l		List of comma-separated descriptions for each list of ranges.
	 */
	public Decode(Map<String, Ranges> m, List<String> l) {
		super(m, l);
	}
	
	/**
	 * Returns the Map of field names to {@link Ranges} object
	 * @return
	 */
	public Map<String, Ranges>  getFields() {
		return fields;
	}
	
	/**
	 * Returns the List of comma-separated descriptions for each list of ranges.
	 * @return
	 */
	public List<String> getDescriptions() {
		return fieldDescriptions;
	}

	/**
	 * Returns the {@link Ranges} associated with the specified field.
	 * @param fieldName		the name of the field
	 * @return
	 */
	public Ranges getRanges(String fieldName) {
		return fields.get(fieldName);
	}
	
	/**
	 * Returns a specific range ({@link MinMax}) for the specified field.
	 * @param fieldName		the name of the field
	 * @param index			the index of the range to return
	 * @return
	 */
	public MinMax getRange(String fieldName, int index) {
		return fields.get(fieldName).getRange(index);
	}
}
