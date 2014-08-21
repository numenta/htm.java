/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.research;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.numenta.nupic.data.ArrayUtils;
import org.numenta.nupic.data.MersenneTwister;
import org.numenta.nupic.data.SparseBinaryMatrix;
import org.numenta.nupic.data.SparseDoubleMatrix;
import org.numenta.nupic.data.SparseMatrix;
import org.numenta.nupic.data.SparseObjectMatrix;
import org.numenta.nupic.model.Column;



public class SpatialPooler {
	private int[] inputDimensions = new int[] { 32, 32 };
	private int[] columnDimensions = new int[] { 64, 64 };
	private int potentialRadius = 16;
	private double potentialPct = 0.5;
	private boolean globalInhibition = false;
	private double localAreaDensity = -1.0;
	private double numActiveColumnsPerInhArea;
	private double stimulusThreshold = 0;
	private double synPermInactiveDec = 0.01;
	private double synPermActiveInc = 0.10;
	private double synPermConnected = 0.10;
	private double minPctOverlapDutyCycle = 0.001;
	private double minPctActiveDutyCycle = 0.001;
	private double dutyCyclePeriod = 1000;
	private double maxBoost = 10.0;
	private int spVerbosity = 0;
	private int seed;
	
	private int numInputs = 1;  //product of input dimensions
	private int numColumns = 1;	//product of column dimensions
	
	//Extra parameter settings
	private double synPermMin = 0.0;
	private double synPermMax = 1.0;
	private double synPermThreshold = synPermActiveInc / 2.0;
	private int updatePeriod = 50;
	private double initConnectedPct = 0.5;
	
	//Internal state
	private double version = 1.0;
	private int interationNum = 0;
	private int iterationLearnNum = 0;
	/**
	 * Store the set of all inputs that are within each column's potential pool.
     * 'potentialPools' is a matrix, whose rows represent cortical columns, and
     * whose columns represent the input bits. if potentialPools[i][j] == 1,
     * then input bit 'j' is in column 'i's potential pool. A column can only be
     * connected to inputs in its potential pool. The indices refer to a
     * flattened version of both the inputs and columns. Namely, irrespective
     * of the topology of the inputs and columns, they are treated as being a
     * one dimensional array. Since a column is typically connected to only a
     * subset of the inputs, many of the entries in the matrix are 0. Therefore
     * the potentialPool matrix is stored using the SparseBinaryMatrix
     * class, to reduce memory footprint and computation time of algorithms that
     * require iterating over the data structure.
     */
	private SparseBinaryMatrix potentialPools;
	/**
	 * Initialize the permanences for each column. Similar to the
     * 'self._potentialPools', the permanences are stored in a matrix whose rows
     * represent the cortical columns, and whose columns represent the input
     * bits. If self._permanences[i][j] = 0.2, then the synapse connecting
     * cortical column 'i' to input bit 'j'  has a permanence of 0.2. Here we
     * also use the SparseMatrix class to reduce the memory footprint and
     * computation time of algorithms that require iterating over the data
     * structure. This permanence matrix is only allowed to have non-zero
     * elements where the potential pool is non-zero.
     */
	private SparseDoubleMatrix<double[]> permanences;
	/**
	 * Initialize a tiny random tie breaker. This is used to determine winning
     * columns where the overlaps are identical.
     */
	private SparseDoubleMatrix<double[]> tieBreaker;
	/**
	 * 'self._connectedSynapses' is a similar matrix to 'self._permanences'
     * (rows represent cortical columns, columns represent input bits) whose
     * entries represent whether the cortical column is connected to the input
     * bit, i.e. its permanence value is greater than 'synPermConnected'. While
     * this information is readily available from the 'self._permanence' matrix,
     * it is stored separately for efficiency purposes.
     */
	private SparseBinaryMatrix connectedSynapses;
	/** The main pooler data structure containing the cortical object model. */
	private SparseObjectMatrix<Column> poolerMemory;
	/** A matrix representing the shape of the input. */
	private SparseBinaryMatrix inputMatrix;
	/** 
	 * Stores the number of connected synapses for each column. This is simply
     * a sum of each row of 'self._connectedSynapses'. again, while this
     * information is readily available from 'self._connectedSynapses', it is
     * stored separately for efficiency purposes.
	 */
	private int[] connectedCounts = new int[numColumns];
	
	private Random random = new MersenneTwister(42);
	
	public SpatialPooler() {
		this(null);
	}
	
	public SpatialPooler(Parameters params) {
		if(params != null) {
			Parameters.apply(this, params);
		}
		
		poolerMemory = new SparseObjectMatrix<Column>(columnDimensions);
		inputMatrix = new SparseBinaryMatrix(inputDimensions);
		
		for(int i = 0;i < inputDimensions.length;i++) {
			numInputs *= inputDimensions[i];
		}
		for(int i = 0;i < columnDimensions.length;i++) {
			numColumns *= columnDimensions[i];
		}
		
		potentialPools = new SparseBinaryMatrix(new int[] { numColumns, numInputs } );
		
		permanences = new SparseDoubleMatrix<double[]>(new int[] { numColumns, numInputs } );
		
		tieBreaker = new SparseDoubleMatrix<double[]>(new int[] { numColumns, numInputs } );
		for(int i = 0;i < numColumns;i++) {
			for(int j = 0;j < numInputs;j++) {
				tieBreaker.set(new int[] { i, j }, 0.01 * random.nextDouble());
			}
		}
		/**
		 * 'self._connectedSynapses' is a similar matrix to 'self._permanences'
	     * (rows represent cortical columns, columns represent input bits) whose
	     * entries represent whether the cortical column is connected to the input
	     * bit, i.e. its permanence value is greater than 'synPermConnected'. While
	     * this information is readily available from the 'self._permanence' matrix,
	     * it is stored separately for efficiency purposes.
	     */
		connectedSynapses = new SparseBinaryMatrix(new int[] { numColumns, numInputs } );
		
		connectedCounts = new int[numColumns];
		// Initialize the set of permanence values for each column. Ensure that
	    // each column is connected to enough input bits to allow it to be
	    // activated.
		for(int i = 0;i < numColumns;i++) {
			//potential = mapPotential(i, true);
		}
	}
	
	/**
	 * Returns the product of the input dimensions 
	 * @return	the product of the input dimensions 
	 */
	public int getNumInputs() {
		return numInputs;
	}
	
	/**
	 * Returns the product of the column dimensions 
	 * @return	the product of the column dimensions 
	 */
	public int getNumColumns() {
		return numColumns;
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
	
	/**
	 * This parameter determines the extent of the input
     * that each column can potentially be connected to.
     * This can be thought of as the input bits that
     * are visible to each column, or a 'receptiveField' of
     * the field of vision. A large enough value will result
     * in 'global coverage', meaning that each column
     * can potentially be connected to every input bit. This
     * parameter defines a square (or hyper square) area: a
     * column will have a max square potential pool with
     * sides of length 2 * potentialRadius + 1.
     * 
	 * @param potentialRadius
	 */
	public void setPotentialRadius(int potentialRadius) {
		this.potentialRadius = potentialRadius;
	}
	
	/**
	 * Returns the configured potential radius
	 * @return	the configured potential radius
	 * @see {@link #setPotentialRadius(int)}
	 */
	public int getPotentialRadius() {
		return potentialRadius;
	}

	/**
	 * The percent of the inputs, within a column's
     * potential radius, that a column can be connected to.
     * If set to 1, the column will be connected to every
     * input within its potential radius. This parameter is
     * used to give each column a unique potential pool when
     * a large potentialRadius causes overlap between the
     * columns. At initialization time we choose
     * ((2*potentialRadius + 1)^(# inputDimensions) *
     * potentialPct) input bits to comprise the column's
     * potential pool.
     * 
	 * @param potentialPct
	 */
	public void setPotentialPct(double potentialPct) {
		this.potentialPct = potentialPct;
	}
	
	/**
	 * Returns the configured potential pct
	 * 
	 * @return the configured potential pct
	 * @see {@link #setPotentialPct(double)}
	 */
	public double getPotentialPct() {
		return potentialPct;
	}

	/**
	 * If true, then during inhibition phase the winning
     * columns are selected as the most active columns from
     * the region as a whole. Otherwise, the winning columns
     * are selected with respect to their local
     * neighborhoods. Using global inhibition boosts
     * performance x60.
     * 
	 * @param globalInhibition
	 */
	public void setGlobalInhibition(boolean globalInhibition) {
		this.globalInhibition = globalInhibition;
	}
	
	/**
	 * Returns the configured global inhibition flag
	 * @return	the configured global inhibition flag
	 * @see {@link #setGlobalInhibition(boolean)}
	 */
	public boolean getGlobalInhibition() {
		return globalInhibition;
	}

	/**
	 * The desired density of active columns within a local
     * inhibition area (the size of which is set by the
     * internally calculated inhibitionRadius, which is in
     * turn determined from the average size of the
     * connected potential pools of all columns). The
     * inhibition logic will insure that at most N columns
     * remain ON within a local inhibition area, where N =
     * localAreaDensity * (total number of columns in
     * inhibition area).
     * 
	 * @param localAreaDensity
	 */
	public void setLocalAreaDensity(double localAreaDensity) {
		this.localAreaDensity = localAreaDensity;
	}
	
	/**
	 * Returns the configured local area density
	 * @return	the configured local area density
	 * @see {@link #setLocalAreaDensity(double)}
	 */
	public double getLocalAreaDensity() {
		return localAreaDensity;
	}

	/**
	 * An alternate way to control the density of the active
     * columns. If numActivePerInhArea is specified then
     * localAreaDensity must be less than 0, and vice versa.
     * When using numActivePerInhArea, the inhibition logic
     * will insure that at most 'numActivePerInhArea'
     * columns remain ON within a local inhibition area (the
     * size of which is set by the internally calculated
     * inhibitionRadius, which is in turn determined from
     * the average size of the connected receptive fields of
     * all columns). When using this method, as columns
     * learn and grow their effective receptive fields, the
     * inhibitionRadius will grow, and hence the net density
     * of the active columns will *decrease*. This is in
     * contrast to the localAreaDensity method, which keeps
     * the density of active columns the same regardless of
     * the size of their receptive fields.
     * 
	 * @param numActiveColumnsPerInhArea
	 */
	public void setNumActiveColumnsPerInhArea(double numActiveColumnsPerInhArea) {
		this.numActiveColumnsPerInhArea = numActiveColumnsPerInhArea;
	}
	
	/**
	 * Returns the configured number of active columns per
	 * inhibition area.
	 * @return	the configured number of active columns per
	 * inhibition area.
	 * @see {@link #setNumActiveColumnsPerInhArea(double)}
	 */
	public double getNumActiveColumnsPerInhArea() {
		return numActiveColumnsPerInhArea;
	}

	/**
	 * This is a number specifying the minimum number of
     * synapses that must be on in order for a columns to
     * turn ON. The purpose of this is to prevent noise
     * input from activating columns. Specified as a percent
     * of a fully grown synapse.
     * 
	 * @param stimulusThreshold
	 */
	public void setStimulusThreshold(double stimulusThreshold) {
		this.stimulusThreshold = stimulusThreshold;
	}
	
	/**
	 * Returns the stimulus threshold
	 * @return	the stimulus threshold
	 * @see {@link #setStimulusThreshold(double)}
	 */
	public double getStimulusThreshold() {
		return stimulusThreshold;
	}

	/**
	 * The amount by which an inactive synapse is
     * decremented in each round. Specified as a percent of
     * a fully grown synapse.
     * 
	 * @param synPermInactiveDec
	 */
	public void setSynPermInactiveDec(double synPermInactiveDec) {
		this.synPermInactiveDec = synPermInactiveDec;
	}
	
	/**
	 * Returns the synaptic permanence inactive decrement.
	 * @return	the synaptic permanence inactive decrement.
	 * @see {@link #setSynPermInactiveDec(double)}
	 */
	public double getSynPermInactiveDec() {
		return synPermInactiveDec;
	}

	/**
	 * The amount by which an active synapse is incremented
     * in each round. Specified as a percent of a
     * fully grown synapse.
     * 
	 * @param synPermActiveInc
	 */
	public void setSynPermActiveInc(double synPermActiveInc) {
		this.synPermActiveInc = synPermActiveInc;
	}
	
	/**
	 * Returns the configured active permanence increment
	 * @return the configured active permanence increment
	 * @see {@link #setSynPermActiveInc(double)}
	 */
	public double getSynPermActiveInc() {
		return synPermActiveInc;
	}

	/**
	 * The default connected threshold. Any synapse whose
     * permanence value is above the connected threshold is
     * a "connected synapse", meaning it can contribute to
     * the cell's firing.
     * 
	 * @param minPctOverlapDutyCycle
	 */
	public void setSynPermConnected(double synPermConnected) {
		this.synPermConnected = synPermConnected;
	}
	
	/**
	 * Returns the synapse permanence connected threshold
	 * @return the synapse permanence connected threshold
	 * @see {@link #setSynPermConnected(double)}
	 */
	public double getSynPermConnected() {
		return synPermConnected;
	}

	/**
	 * A number between 0 and 1.0, used to set a floor on
     * how often a column should have at least
     * stimulusThreshold active inputs. Periodically, each
     * column looks at the overlap duty cycle of
     * all other columns within its inhibition radius and
     * sets its own internal minimal acceptable duty cycle
     * to: minPctDutyCycleBeforeInh * max(other columns'
     * duty cycles).
     * On each iteration, any column whose overlap duty
     * cycle falls below this computed value will  get
     * all of its permanence values boosted up by
     * synPermActiveInc. Raising all permanences in response
     * to a sub-par duty cycle before  inhibition allows a
     * cell to search for new inputs when either its
     * previously learned inputs are no longer ever active,
     * or when the vast majority of them have been
     * "hijacked" by other columns.
     * 
	 * @param minPctOverlapDutyCycle
	 */
	public void setMinPctOverlapDutyCycle(double minPctOverlapDutyCycle) {
		this.minPctOverlapDutyCycle = minPctOverlapDutyCycle;
	}
	
	/**
	 * {@see #setMinPctOverlapDutyCycle(double)}
	 * @return
	 */
	public double getMinPctOverlapDutyCycle() {
		return minPctOverlapDutyCycle;
	}

	/**
	 * A number between 0 and 1.0, used to set a floor on
     * how often a column should be activate.
     * Periodically, each column looks at the activity duty
     * cycle of all other columns within its inhibition
     * radius and sets its own internal minimal acceptable
     * duty cycle to:
     *   minPctDutyCycleAfterInh *
     *   max(other columns' duty cycles).
     * On each iteration, any column whose duty cycle after
     * inhibition falls below this computed value will get
     * its internal boost factor increased.
     * 
	 * @param minPctActiveDutyCycle
	 */
	public void setMinPctActiveDutyCycle(double minPctActiveDutyCycle) {
		this.minPctActiveDutyCycle = minPctActiveDutyCycle;
	}
	
	/**
	 * Returns the minPctActiveDutyCycle
	 * @return	the minPctActiveDutyCycle
	 * @see {@link #setMinPctActiveDutyCycle(double)}
	 */
	public double getMinPctActiveDutyCycle() {
		return minPctActiveDutyCycle;
	}

	/**
	 * The period used to calculate duty cycles. Higher
     * values make it take longer to respond to changes in
     * boost or synPerConnectedCell. Shorter values make it
     * more unstable and likely to oscillate.
     * 
	 * @param dutyCyclePeriod
	 */
	public void setDutyCyclePeriod(double dutyCyclePeriod) {
		this.dutyCyclePeriod = dutyCyclePeriod;
	}
	
	/**
	 * Returns the configured duty cycle period
	 * @return	the configured duty cycle period
	 * @see {@link #setDutyCyclePeriod(double)}
	 */
	public double getDutyCyclePeriod() {
		return dutyCyclePeriod;
	}

	/**
	 * The maximum overlap boost factor. Each column's
     * overlap gets multiplied by a boost factor
     * before it gets considered for inhibition.
     * The actual boost factor for a column is number
     * between 1.0 and maxBoost. A boost factor of 1.0 is
     * used if the duty cycle is >= minOverlapDutyCycle,
     * maxBoost is used if the duty cycle is 0, and any duty
     * cycle in between is linearly extrapolated from these
     * 2 endpoints.
     * 
	 * @param maxBoost
	 */
	public void setMaxBoost(double maxBoost) {
		this.maxBoost = maxBoost;
	}
	
	/**
	 * Returns the max boost
	 * @return	the max boost
	 * @see {@link #setMaxBoost(double)}
	 */
	public double getMaxBoost() {
		return maxBoost;
	}
	
	/**
	 * Sets the seed used by the random number generator
	 * @param seed
	 */
	public void setSeed(int seed) {
		this.seed = seed;
	}
	
	/**
	 * Returns the seed used to configure the random generator
	 * @return
	 */
	public int getSeed() {
		return seed;
	}

	/**
	 * spVerbosity level: 0, 1, 2, or 3
	 * 
	 * @param spVerbosity
	 */
	public void setSpVerbosity(int spVerbosity) {
		this.spVerbosity = spVerbosity;
	}
	
	/**
	 * Returns the verbosity setting.
	 * @return	the verbosity setting.
	 * @see {@link #setSpVerbosity(int)}
	 */
	public int getSpVerbosity() {
		return spVerbosity;
	}
	
	/**
	 * Maps a column to its respective input index, keeping to the topology of
     * the region. It takes the index of the column as an argument and determines
     * what is the index of the flattened input vector that is to be the center of
     * the column's potential pool. It distributes the columns over the inputs
     * uniformly. The return value is an integer representing the index of the
     * input bit. Examples of the expected output of this method:
     * * If the topology is one dimensional, and the column index is 0, this
     *   method will return the input index 0. If the column index is 1, and there
     *   are 3 columns over 7 inputs, this method will return the input index 3.
     * * If the topology is two dimensional, with column dimensions [3, 5] and
     *   input dimensions [7, 11], and the column index is 3, the method
     *   returns input index 8. 
     *   
	 * @param columnIndex	The index identifying a column in the permanence, potential
                    		and connectivity matrices.
	 * @return				A boolean value indicating that boundaries should be
                    		ignored.
	 */
	public int mapColumn(int columnIndex) {
		int[] columnCoords = poolerMemory.computeCoordinates(columnIndex);
		double[] colCoords = ArrayUtils.toDoubleArray(columnCoords);
		double[] ratios = ArrayUtils.divide(
			colCoords, ArrayUtils.toDoubleArray(columnDimensions), 0, -1);
		double[] inputCoords = ArrayUtils.multiply(
				ArrayUtils.toDoubleArray(inputDimensions), ratios, -1, 0);
		int[] inputCoordInts = ArrayUtils.toIntArray(inputCoords);
		int inputIndex = inputMatrix.computeIndex(inputCoordInts);
		return inputIndex;
	}
	
	/**
	 * Maps a column to its input bits. This method encapsulates the topology of
     * the region. It takes the index of the column as an argument and determines
     * what are the indices of the input vector that are located within the
     * column's potential pool. The return value is a list containing the indices
     * of the input bits. The current implementation of the base class only
     * supports a 1 dimensional topology of columns with a 1 dimensional topology
     * of inputs. To extend this class to support 2-D topology you will need to
     * override this method. Examples of the expected output of this method:
     * * If the potentialRadius is greater than or equal to the entire input
     *   space, (global visibility), then this method returns an array filled with
     *   all the indices
     * * If the topology is one dimensional, and the potentialRadius is 5, this
     *   method will return an array containing 5 consecutive values centered on
     *   the index of the column (wrapping around if necessary).
     * * If the topology is two dimensional (not implemented), and the
     *   potentialRadius is 5, the method should return an array containing 25
     *   '1's, where the exact indices are to be determined by the mapping from
     *   1-D index to 2-D position.
	 *                 
	 * @param index			The index identifying a column in the permanence, potential
     *                 		and connectivity matrices.
	 * @param wrapAround	A boolean value indicating that boundaries should be
     *                 		ignored.
	 * @return
	 */
	public int[] mapPotential(int columnIndex, boolean wrapAround) {
		int inputIndex = mapColumn(columnIndex);
		return null;
	}
	
	/**
	 * Similar to _getNeighbors1D and _getNeighbors2D (Not included in this implementation), 
	 * this function Returns a list of indices corresponding to the neighbors of a given column. 
	 * Since the permanence values are stored in such a way that information about topology
     * is lost. This method allows for reconstructing the topology of the inputs,
     * which are flattened to one array. Given a column's index, its neighbors are
     * defined as those columns that are 'radius' indices away from it in each
     * dimension. The method returns a list of the flat indices of these columns.
     * 
	 * @param poolerMem		matrix configured to this {@code SpatialPooler}'s dimensions 
	 * 						for transformation work.
	 * @param columnIndex	he index identifying a column in the permanence, potential
     *               		and connectivity matrices.
	 * @param radius		Indicates how far away from a given column are other
     *               		columns to be considered its neighbors. In the previous 2x3
     *               		example, each column with coordinates:
     *               		[2+/-radius, 3+/-radius] is considered a neighbor.
	 * @param wrapAround	A boolean value indicating whether to consider columns at
     *               		the border of a dimensions to be adjacent to columns at the
     *               		other end of the dimension. For example, if the columns are
     *               		laid out in one dimension, columns 1 and 10 will be
     *               		considered adjacent if wrapAround is set to true:
     *               		[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
     *               
	 * @return				a list of the flat indices of these columns
	 */
	public <M extends SparseMatrix> int[] getNeighborsND(M poolerMem, int columnIndex, int radius, boolean wrapAround) {
		int[] columnCoords = poolerMem.computeCoordinates(columnIndex);
		List<int[]> dimensionCoords = new ArrayList<int[]>();
		for(int i = 0;i < inputDimensions.length;i++) {
			int[] range = ArrayUtils.range(columnCoords[i] - radius, columnCoords[i] + radius + 1);
			int[] curRange = new int[range.length];
			
			if(wrapAround) {
				for(int j = 0;j < curRange.length;j++) {
					curRange[j] = (int)ArrayUtils.positiveRemainder(range[j], inputDimensions[i]);
				}
			}else{
				curRange = range;
			}
			
			dimensionCoords.add(ArrayUtils.unique(curRange));
		}
		
		List<TIntList> neighborList = ArrayUtils.dimensionsToCoordinateList(dimensionCoords);
		TIntList neighbors = new TIntArrayList(neighborList.size());
		for(int i = 0;i < neighborList.size();i++) {
			int flatIndex = poolerMem.computeIndex(neighborList.get(i).toArray());
			if(flatIndex == columnIndex) continue;
			neighbors.add(flatIndex);
		}
		return neighbors.toArray();
	}
}
