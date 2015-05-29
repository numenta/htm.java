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
