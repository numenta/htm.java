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
import java.util.Arrays;
import java.util.List;

import org.numenta.nupic.data.ArrayUtils;
import org.numenta.nupic.model.Lattice;



public class SpatialPooler {
	
	public SpatialPooler() {
		
	}
	
	/**
	 * This method updates the permanence matrix with a column's new permanence
     * values. The column is identified by its index, which reflects the row in
     * the matrix, and the permanence is given in 'dense' form, i.e. a full
     * array containing all the zeros as well as the non-zero values. It is in
     * charge of implementing 'clipping' - ensuring that the permanence values are
     * always between 0 and 1 - and 'trimming' - enforcing sparseness by zeroing out
     * all permanence values below '_synPermTrimThreshold'. It also maintains
     * the consistency between 'self._permanences' (the matrix storing the
     * permanence values), 'self._connectedSynapses', (the matrix storing the bits
     * each column is connected to), and 'self._connectedCounts' (an array storing
     * the number of input bits each column is connected to). Every method wishing
     * to modify the permanence matrix should do so through this method.
     * 
     * @param l					the {@link Lattice} which is the memory model.
	 * @param perm				An array of permanence values for a column. The array is
     *               			"dense", i.e. it contains an entry for each input bit, even
     *               			if the permanence value is 0.
	 * @param columnIndex		The index identifying a column in the permanence, potential
     *               			and connectivity matrices
	 * @param raisePerm			a boolean value indicating whether the permanence values
     *               			should be raised until a minimum number are synapses are in
     *               			a connected state. Should be set to 'false' when a direct
     *               			assignment is required.
	 */
	public static void updatePermanencesForColumn(Lattice l, double[] perm, int columnIndex, boolean raisePerm) {
		
	}
	
	/**
	 * Returns a randomly generated permanence value for a synapses that is
     * initialized in a connected state. The basic idea here is to initialize
     * permanence values very close to synPermConnected so that a small number of
     * learning steps could make it disconnected or connected.
     *
     * Note: experimentation was done a long time ago on the best way to initialize
     * permanence values, but the history for this particular scheme has been lost.
     * 
	 * @return	a randomly generated permanence value
	 */
	public static double initPermConnected(SpatialLattice l) {
		double p = l.getSynPermConnected() + l.getRandom().nextDouble() * l.getSynPermActiveInc() / 4.0;
		p = ((int)p * 100000) / 100000.0d;
		return p;
	}
	
	/**
	 * Returns a randomly generated permanence value for a synapses that is to be
     * initialized in a non-connected state.
     * 
	 * @return	a randomly generated permanence value
	 */
	public static double initPermNonConnected(SpatialLattice l) {
		double p = l.getSynPermConnected() * l.getRandom().nextDouble();
		p = ((int)p * 100000) / 100000.0d;
		return p;
	}
	/**
	 * Initializes the permanences of a column. The method
     * returns a 1-D array the size of the input, where each entry in the
     * array represents the initial permanence value between the input bit
     * at the particular index in the array, and the column represented by
     * the 'index' parameter.
     * 
     * @param l					the {@link SpatialLattice} which is the memory model
	 * @param potentialPool		An array specifying the potential pool of the column.
     *               			Permanence values will only be generated for input bits
     *               			corresponding to indices for which the mask value is 1.
	 * @param connectedPct		A value between 0 or 1 specifying the percent of the input
     *               			bits that will start off in a connected state.
	 * @return
	 */
	public static double[] initPermanence(SpatialLattice l, int[] potentialPool, double connectedPct) {
		double[] perm = new double[l.getNumInputs()];
		Arrays.fill(perm, 0);
		for(int i = 0;i < potentialPool.length;i++) {
			if(l.getRandom().nextDouble() <= connectedPct) {
				perm[i] = initPermConnected(l);
			}else{
				perm[i] = initPermNonConnected(l);
			}
			
			perm[i] = perm[i] < l.getSynPermTrimThreshold() ? 0 : perm[i];
		}
		
		return perm;
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
     *               		and connectivity matrices.
	 * @return				A boolean value indicating that boundaries should be
     *               		ignored.
	 */
	public static int mapColumn(Lattice l, int columnIndex) {
		int[] columnCoords = l.getMemory().computeCoordinates(columnIndex);
		double[] colCoords = ArrayUtils.toDoubleArray(columnCoords);
		double[] ratios = ArrayUtils.divide(
			colCoords, ArrayUtils.toDoubleArray(l.getColumnDimensions()), 0, -1);
		double[] inputCoords = ArrayUtils.multiply(
				ArrayUtils.toDoubleArray(l.getInputDimensions()), ratios, -1, 0);
		int[] inputCoordInts = ArrayUtils.toIntArray(inputCoords);
		int inputIndex = l.getInputMatrix().computeIndex(inputCoordInts);
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
	 * @param	l			{@link SpatialLattice} the main memory model
	 * @param index			The index identifying a column in the permanence, potential
     *                 		and connectivity matrices.
	 * @param wrapAround	A boolean value indicating that boundaries should be
     *                 		ignored.
	 * @return
	 */
	public static int[] mapPotential(SpatialLattice l, int columnIndex, boolean wrapAround) {
		int inputIndex = mapColumn(l, columnIndex);
		
		TIntArrayList indices = getNeighborsND(l, inputIndex, l.getPotentialRadius(), wrapAround);
		indices.add(inputIndex);
		indices.sort();
		
		int[] sample = ArrayUtils.sample((int)Math.round(indices.size() * l.getPotentialPct()), indices, l.getRandom());
		
		return sample;
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
	public static TIntArrayList getNeighborsND(SpatialLattice l, int columnIndex, int radius, boolean wrapAround) {
		int[] columnCoords = l.getInputMatrix().computeCoordinates(columnIndex);
		List<int[]> dimensionCoords = new ArrayList<int[]>();
		for(int i = 0;i < l.getInputDimensions().length;i++) {
			int[] range = ArrayUtils.range(columnCoords[i] - radius, columnCoords[i] + radius + 1);
			int[] curRange = new int[range.length];
			
			if(wrapAround) {
				for(int j = 0;j < curRange.length;j++) {
					curRange[j] = (int)ArrayUtils.positiveRemainder(range[j], l.getInputDimensions()[i]);
				}
			}else{
				curRange = range;
			}
			
			dimensionCoords.add(ArrayUtils.unique(curRange));
		}
		
		List<TIntList> neighborList = ArrayUtils.dimensionsToCoordinateList(dimensionCoords);
		TIntArrayList neighbors = new TIntArrayList(neighborList.size());
		for(int i = 0;i < neighborList.size();i++) {
			int flatIndex = l.getInputMatrix().computeIndex(neighborList.get(i).toArray());
			if(flatIndex == columnIndex) continue;
			neighbors.add(flatIndex);
		}
		return neighbors;
	}
}
