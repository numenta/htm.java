package org.numenta.nupic.encoders;

import java.util.List;

import org.numenta.nupic.util.MinMax;

/**
 * Convenience subclass of {@link Tuple} to contain the list of
 * ranges expressed for a particular decoded output of an
 * {@link Encoder} by using tightly constrained types without 
 * the verbosity at the instantiation site.
 * 
 * @author David Ray
 */
public class Ranges extends RangeTuple<List<MinMax>, String>{

	/**
	 * Constructs and new {@code Ranges} object.
	 * @param l		the {@link List} of {@link MinMax} objects which are the 
	 * 				minimum and maximum postions of 1's
	 * @param s
	 */
	public Ranges(List<MinMax> l, String s) {
		super(l, s);
	}

	/**
	 * Returns a List of the {@link MinMax}es.
	 * @return
	 */
	public List<MinMax> getRanges() {
		return l;
	}
	
	/**
	 * Returns a comma-separated String containing the descriptions
	 * for all of the {@link MinMax}es
	 * @return
	 */
	public String getDescription() {
		return desc;
	}
	
	/**
	 * Adds a {@link MinMax} to this list of ranges
	 * @param mm
	 */
	public void add(MinMax mm) {
		l.add(mm);
	}
	
	/**
	 * Returns the specified {@link MinMax} 
	 * 	
	 * @param index		the index of the MinMax to return
	 * @return			the specified {@link MinMax} 
	 */
	public MinMax getRange(int index) {
		return l.get(index);
	}
	
	/**
	 * Sets the entire comma-separated description string
	 * @param s
	 */
	public void setDescription(String s) {
		this.desc = s;
	}
	 
	/**
	 * Returns the count of ranges contained in this Ranges object
	 * @return
	 */
	public int size() {
		return l.size();
	}
}
