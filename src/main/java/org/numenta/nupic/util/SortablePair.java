package org.numenta.nupic.util;

/**
 * Holds a pair of generic entries which can be treated as 
 * a single unit in which the first argument must be an instance of
 * {@link Comparable} for sorting purposes.
 * 
 * @author David Ray
 *
 * @param <F>	the type of the first argument
 * @param <S>	the type of the second argument
 */
public class SortablePair<F extends Comparable<F>, S> implements Comparable<F> {
	private F first;
	private S second;
	/**
	 * Constructs a new {@code Pair}
	 * @param first		the first argument of type &lt;F&gt; which must be an 
	 * 					instance of {@link Comparable}
	 * @param second	the second argument of type &lt;S&gt;
	 */
	public SortablePair(F first, S second) {
		this.first = first;
		this.second = second;
	}
	/**
	 * Returns the first argument &lt;F&gt;
	 * @return
	 */
	public F first() {
		return first;
	}
	/**
	 * Returns the second argument &lt;S&gt;
	 * @return
	 */
	public S second() {
		return second;
	}
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(F o) {
		return first.compareTo( ((SortablePair<F, S>)o).first);
	}
}
