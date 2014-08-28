package org.numenta.nupic.model;

import java.util.Random;

import org.numenta.nupic.data.MersenneTwister;
import org.numenta.nupic.data.SparseMatrix;

/**
 * Model for the structural elements, state and connectivity of a given cortical 
 * algorithmic layer.
 * 
 * @author David Ray
 *
 */
public abstract class Lattice {
	/** Main memory model and state */
	protected SparseMatrix<?> memory;
	/** A matrix representing the shape of the input. */
	protected SparseMatrix<?> inputMatrix;
	
	protected Random random = new MersenneTwister(42);
	
	//Defaults
	protected int[] inputDimensions = new int[] { 32, 32 };
	protected int[] columnDimensions = new int[] { 64, 64 };
	
	public Lattice() {}
	
	/**
	 * Returns the {@link SparseMatrix} which contains the model elements
	 * 	
	 * @return	the main memory matrix
	 */
	public abstract SparseMatrix<?> getMemory();
	
	/**
	 * Returns the input bit matrix
	 * @return
	 */
	public abstract SparseMatrix<?> getInputMatrix();
	
	/**
	 * Sets the {@link Random} number generator
	 * @param r
	 */
	public void setRandom(Random r) {
		this.random = r;
	}
	
	/**
	 * Returns the random number generator
	 * @return
	 */
	public Random getRandom() {
		return random;
	}
	
	/**
	 * A list representing the dimensions of the input
	 * vector. Format is [height, width, depth, ...], where
	 * each value represents the size of the dimension. For a
	 * topology of one dimension with 100 inputs use 100, or
	 * [100]. For a two dimensional topology of 10x5 use
	 * [10,5].
	 * 
	 * @param inputDimensions
	 */
	public void setInputDimensions(int[] inputDimensions) {
		this.inputDimensions = inputDimensions;
	}
	
	/**
	 * Returns the configured input dimensions
	 *
	 * @return the configured input dimensions
	 * @see {@link #setInputDimensions(int[])}
	 */
	public int[] getInputDimensions() {
		return inputDimensions;
	}
	
	/**
	 * A list representing the dimensions of the columns in
	 * the region. Format is [height, width, depth, ...],
	 * where each value represents the size of the dimension.
	 * For a topology of one dimension with 2000 columns use
	 * 2000, or [2000]. For a three dimensional topology of
	 * 32x64x16 use [32, 64, 16].
	 * 
	 * @param columnDimensions
	 */
	public void setColumnDimensions(int[] columnDimensions) {
		this.columnDimensions = columnDimensions;
	}
	
	/**
	 * Returns the configured column dimensions.
	 * 
	 * @return	the configured column dimensions.
	 * @see {@link #setColumnDimensions(int[])}
	 */
	public int[] getColumnDimensions() {
		return columnDimensions;
	}
}
