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

import java.util.List;
import java.util.Map;

import org.numenta.nupic.util.DecodeTuple;
import org.numenta.nupic.util.MinMax;

/**
 * Tuple to contain the results of an {@link Encoder}'s decoded
 * values.
 * 
 * @author David Ray
 */
public class DecodeResult extends DecodeTuple<Map<String, RangeList>, List<String>> {
	
	private static final long serialVersionUID = 1L;

    /**
	 * Constructs a new {@code Decode}
	 * @param m		Map of field names to {@link RangeList} object
	 * @param l		List of comma-separated descriptions for each list of ranges.
	 */
	public DecodeResult(Map<String, RangeList> m, List<String> l) {
		super(m, l);
	}
	
	/**
	 * Returns the Map of field names to {@link RangeList} object
	 * @return
	 */
	public Map<String, RangeList>  getFields() {
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
	 * Returns the {@link RangeList} associated with the specified field.
	 * @param fieldName		the name of the field
	 * @return
	 */
	public RangeList getRanges(String fieldName) {
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
