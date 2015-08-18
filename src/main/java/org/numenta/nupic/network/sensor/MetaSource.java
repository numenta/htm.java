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
package org.numenta.nupic.network.sensor;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.encoders.MultiEncoder;

/**
 * Abstraction of a basic entry format for input into an Encoder,
 * with specific bias to a {@link MultiEncoder}'s format of {@link Map}
 * input.
 * 
 * @author David Ray
 * @see MultiEncoder#encodeIntoArray(Object, int[])
 */
public interface MetaSource {
	/**
	 * Retrieves the 3 line header specifying:
	 * <pre>
	 *     1. The field names
	 *     2. The field types
	 *     3. The field flags
	 * </pre>
	 * @return
	 */
	public List<String[]> getHeader();
	/**
	 * The line by line (array of columnated Strings) content
	 * of a given file.
	 * @return
	 */
	public List<String[]> getBody();
	/**
	 * Returns a Map adhering to the input format of a
	 * MultiEncoder, namely:
	 * <pre>
	 *     key=fieldName, value=Object
	 * </pre>
	 * 
	 * "Smart" iterator implementations will reuse the same Map
	 * on every call to {@link Iterator#next()}
	 * 
	 * @return
	 */
	public Iterator<Map<String, Object>> multiIterator();
}
