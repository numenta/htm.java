package org.numenta.nupic.algorithms;

public class CLAClassifier {
	int verbosity = 0;
	
	double alpha;
	/** 
	 * The bit's learning iteration. This is updated each time store() gets
     * called on this bit.
	 */
	private int learnIteration;
}
