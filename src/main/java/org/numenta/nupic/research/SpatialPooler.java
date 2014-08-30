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
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.numenta.nupic.data.ArrayUtils;
import org.numenta.nupic.data.ArrayUtils.Condition;
import org.numenta.nupic.data.SparseMatrix;
import org.numenta.nupic.model.Lattice;



public class SpatialPooler {
    
    public SpatialPooler() {
        
    }
    
    /**
     * This is the primary public method of the SpatialPooler class. This
     * function takes a input vector and outputs the indices of the active columns.
     * If 'learn' is set to True, this method also updates the permanences of the
     * columns. 
     * 
     * @param l
     * @param inputVector       A <del>numpy</del> array of 0's and 1's that comprises the input to
     *                          the spatial pooler. The array will be treated as a one
     *                          dimensional array, therefore the dimensions of the array
     *                          do not have to match the exact dimensions specified in the
     *                          class constructor. In fact, even a list would suffice.
     *                          The number of input bits in the vector must, however,
     *                          match the number of bits specified by the call to the
     *                          constructor. Therefore there must be a '0' or '1' in the
     *                          array for every input bit.
     * @param learn             A boolean value indicating whether learning should be
     *                          performed. Learning entails updating the  permanence
     *                          values of the synapses, and hence modifying the 'state'
     *                          of the model. Setting learning to 'off' freezes the SP
     *                          and has many uses. For example, you might want to feed in
     *                          various inputs and examine the resulting SDR's.
     * @param activeArray       An array whose size is equal to the number of columns.
     *                          Before the function returns this array will be populated
     *                          with 1's at the indices of the active columns, and 0's
     *                          everywhere else.
     */
    public void compute(SpatialLattice l, int[] inputVector, boolean learn, int[] activeArray) {
        if(inputVector.length != l.getNumInputs()) {
            throw new IllegalArgumentException("Input array must be same size as the defined number of inputs");
        }
        
        updateBookeepingVars(l, learn);
        calculateOverlap(l, inputVector);
    }
    
    /**
     * The range of connectedSynapses per column, averaged for each dimension.
     * This value is used to calculate the inhibition radius. This variation of
     * the function supports arbitrary column dimensions.
     *  
     * @param l             the {@link SpatialLattice} (spatial pooler memory)
     * @param columnIndex   the current column for which to avg.
     * @return
     */
    public static double avgConnectedSpanForColumnND(SpatialLattice l, int columnIndex) {
        int[] dimensions = l.getInputDimensions();
        int[] connected = l.getConnectedSynapses().getObject(columnIndex);
        if(connected.length == 0) return 0;
        
        int[] maxCoord = new int[l.getInputDimensions().length];
        int[] minCoord = new int[l.getInputDimensions().length];
        Arrays.fill(maxCoord, -1);
        Arrays.fill(minCoord, ArrayUtils.max(dimensions));
        SparseMatrix<?> inputMatrix = l.getInputMatrix();
        for(int i = 0;i < connected.length;i++) {
            maxCoord = ArrayUtils.maxBetween(maxCoord, inputMatrix.computeCoordinates(connected[i]));
            minCoord = ArrayUtils.minBetween(minCoord, inputMatrix.computeCoordinates(connected[i]));
        }
        return ArrayUtils.average(ArrayUtils.add(ArrayUtils.subtract(maxCoord, minCoord), 1));
    }
    
    /**
     * Update the inhibition radius. The inhibition radius is a measure of the
     * square (or hypersquare) of columns that each a column is "connected to"
     * on average. Since columns are are not connected to each other directly, we
     * determine this quantity by first figuring out how many *inputs* a column is
     * connected to, and then multiplying it by the total number of columns that
     * exist for each input. For multiple dimension the aforementioned
     * calculations are averaged over all dimensions of inputs and columns. This
     * value is meaningless if global inhibition is enabled.
     * 
     * @param l
     */
    public static void updateInhibitionRadius(SpatialLattice l) {
        if(l.getGlobalInhibition()) {
            l.setInhibitionRadius(l.getMemory().getMaxIndex());
            return;
        }
        
        TDoubleArrayList avgCollected = new TDoubleArrayList();
        int len = l.getNumColumns();
        for(int i = 0;i < len;i++) {
            avgCollected.add(avgConnectedSpanForColumnND(l, i));
        }
        double avgConnectedSpan = ArrayUtils.average(avgCollected.toArray());
        double diameter = avgConnectedSpan * avgColumnsPerInput(l);
        double radius = (diameter - 1) / 2.0d;
        radius = Math.max(1, radius);
        l.setInhibitionRadius((int)Math.round(radius));
    }
    
    /**
     * The average number of columns per input, taking into account the topology
     * of the inputs and columns. This value is used to calculate the inhibition
     * radius. This function supports an arbitrary number of dimensions. If the
     * number of column dimensions does not match the number of input dimensions,
     * we treat the missing, or phantom dimensions as 'ones'.
     *  
     * @param l
     * @return
     */
    public static double avgColumnsPerInput(SpatialLattice l) {
        int[] colDim = Arrays.copyOf(l.getColumnDimensions(), l.getColumnDimensions().length);
        int[] inputDim = Arrays.copyOf(l.getInputDimensions(), l.getInputDimensions().length);
        double[] columnsPerInput = ArrayUtils.divide(
            ArrayUtils.toDoubleArray(colDim), ArrayUtils.toDoubleArray(inputDim), 0, 0);
        return ArrayUtils.average(columnsPerInput);
    }
    
    /**
     * This method ensures that each column has enough connections to input bits
     * to allow it to become active. Since a column must have at least
     * 'self._stimulusThreshold' overlaps in order to be considered during the
     * inhibition phase, columns without such minimal number of connections, even
     * if all the input bits they are connected to turn on, have no chance of
     * obtaining the minimum threshold. For such columns, the permanence values
     * are increased until the minimum number of connections are formed.
     * 
     * @param l
     * @param perm
     * @param mask
     */
    public static void raisePermanenceToThreshold(SpatialLattice l, double[] perm, int[] mask) {
        ArrayUtils.clip(perm, l.getSynPermMin(), l.getSynPermMax());
        while(true) {
            int numConnected = ArrayUtils.valueGreaterCount(l.getSynPermConnected(), perm);
            if(numConnected >= l.getStimulusThreshold()) return;
            
            ArrayUtils.raiseValuesBy(l.getSynPermBelowStimulusInc(), perm);
        }
    }
    
    /**
     * This method updates the permanence matrix with a column's new permanence
     * values. The column is identified by its index, which reflects the row in
     * the matrix, and the permanence is given in 'dense' form, i.e. a full
     * array containing all the zeros as well as the non-zero values. It is in
     * charge of implementing 'clipping' - ensuring that the permanence values are
     * always between 0 and 1 - and 'trimming' - enforcing sparseness by zeroing out
     * all permanence values below 'synPermTrimThreshold'. It also maintains
     * the consistency between 'permanences' (the matrix storing the
     * permanence values), 'connectedSynapses', (the matrix storing the bits
     * each column is connected to), and 'connectedCounts' (an array storing
     * the number of input bits each column is connected to). Every method wishing
     * to modify the permanence matrix should do so through this method.
     * 
     * @param l                 the {@link Lattice} which is the memory model.
     * @param perm              An array of permanence values for a column. The array is
     *                          "dense", i.e. it contains an entry for each input bit, even
     *                          if the permanence value is 0.
     * @param columnIndex       The index identifying a column in the permanence, potential
     *                          and connectivity matrices
     * @param raisePerm         a boolean value indicating whether the permanence values
     *                          should be raised until a minimum number are synapses are in
     *                          a connected state. Should be set to 'false' when a direct
     *                          assignment is required.
     */
    public static void updatePermanencesForColumn(SpatialLattice l, double[] perm, int columnIndex, boolean raisePerm) {
        int[] maskPotential = l.getPotentialPools().getObject(columnIndex); 
        if(raisePerm) {
            raisePermanenceToThreshold(l, perm, maskPotential);
        }
        
        ArrayUtils.lessThanXThanSetToY(perm, l.getSynPermTrimThreshold(), 0);
        ArrayUtils.clip(perm, l.getSynPermMin(), l.getSynPermMax());
        TIntArrayList newConnected = new TIntArrayList();
        for(int i = 0;i < perm.length;i++) {
            if(perm[i] >= l.getSynPermConnected()) {
                newConnected.add(i);
            }
        }
        l.getPermanences().set(l.getPermanences().computeCoordinates(columnIndex), perm);
        l.getConnectedSynapses().set(columnIndex, newConnected.toArray());
        l.getConnectedCounts()[columnIndex] = newConnected.size();
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
     * @return  a randomly generated permanence value
     */
    public static double initPermConnected(SpatialLattice l) {
        double p = l.getSynPermConnected() + l.getRandom().nextDouble() * l.getSynPermActiveInc() / 4.0;
        p = ((int)(p * 100000)) / 100000.0d;
        return p;
    }
    
    /**
     * Returns a randomly generated permanence value for a synapses that is to be
     * initialized in a non-connected state.
     * 
     * @return  a randomly generated permanence value
     */
    public static double initPermNonConnected(SpatialLattice l) {
        double p = l.getSynPermConnected() * l.getRandom().nextDouble();
        p = ((int)(p * 100000)) / 100000.0d;
        return p;
    }
    /**
     * Initializes the permanences of a column. The method
     * returns a 1-D array the size of the input, where each entry in the
     * array represents the initial permanence value between the input bit
     * at the particular index in the array, and the column represented by
     * the 'index' parameter.
     * 
     * @param l                 the {@link SpatialLattice} which is the memory model
     * @param potentialPool     An array specifying the potential pool of the column.
     *                          Permanence values will only be generated for input bits
     *                          corresponding to indices for which the mask value is 1.
     * @param connectedPct      A value between 0 or 1 specifying the percent of the input
     *                          bits that will start off in a connected state.
     * @return
     */
    public static double[] initPermanence(SpatialLattice l, TIntHashSet potentialPool, double connectedPct) {
        int len = l.getNumInputs();
        double[] perm = new double[len];
        Arrays.fill(perm, 0);
        for(int i = 0;i < len;i++) {
            if(!potentialPool.contains(i)) continue;
            
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
     * @param columnIndex   The index identifying a column in the permanence, potential
     *                      and connectivity matrices.
     * @return              A boolean value indicating that boundaries should be
     *                      ignored.
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
     * @param   l           {@link SpatialLattice} the main memory model
     * @param index         The index identifying a column in the permanence, potential
     *                      and connectivity matrices.
     * @param wrapAround    A boolean value indicating that boundaries should be
     *                      ignored.
     * @return
     */
    public static int[] mapPotential(SpatialLattice l, int columnIndex, boolean wrapAround) {
        int inputIndex = mapColumn(l, columnIndex);
        
        TIntArrayList indices = getNeighborsND(l, inputIndex, l.getPotentialRadius(), wrapAround);
        indices.add(inputIndex);
        //TODO: See https://github.com/numenta/nupic.core/issues/128
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
     * @param poolerMem     matrix configured to this {@code SpatialPooler}'s dimensions 
     *                      for transformation work.
     * @param columnIndex   he index identifying a column in the permanence, potential
     *                      and connectivity matrices.
     * @param radius        Indicates how far away from a given column are other
     *                      columns to be considered its neighbors. In the previous 2x3
     *                      example, each column with coordinates:
     *                      [2+/-radius, 3+/-radius] is considered a neighbor.
     * @param wrapAround    A boolean value indicating whether to consider columns at
     *                      the border of a dimensions to be adjacent to columns at the
     *                      other end of the dimension. For example, if the columns are
     *                      laid out in one dimension, columns 1 and 10 will be
     *                      considered adjacent if wrapAround is set to true:
     *                      [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
     *               
     * @return              a list of the flat indices of these columns
     */
    public static TIntArrayList getNeighborsND(SpatialLattice l, int columnIndex, int radius, boolean wrapAround) {
        final int[] dimensions = l.getInputDimensions();
        int[] columnCoords = l.getInputMatrix().computeCoordinates(columnIndex);
        List<int[]> dimensionCoords = new ArrayList<int[]>();
        for(int i = 0;i < dimensions.length;i++) {
            int[] range = ArrayUtils.range(columnCoords[i] - radius, columnCoords[i] + radius + 1);
            int[] curRange = new int[range.length];
            
            if(wrapAround) {
                for(int j = 0;j < curRange.length;j++) {
                    curRange[j] = (int)ArrayUtils.positiveRemainder(range[j], dimensions[i]);
                }
            }else{
                final int idx = i;
                curRange = range;
                curRange = ArrayUtils.retainLogicalAnd(range, 
                    new Condition[] {
                        new Condition.Adapter() {
                            @Override public boolean eval(int n) { return n >= 0; }
                        },
                        new Condition.Adapter() {
                            @Override public boolean eval(int n) { return n < dimensions[idx]; }
                        }
                    }
                );
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
    
    /**
     * Updates counter instance variables each cycle.
     *  
     * @param l         the {@link SpatialLattice} memory encapsulation
     * @param learn     a boolean value indicating whether learning should be
     *                  performed. Learning entails updating the  permanence
     *                  values of the synapses, and hence modifying the 'state'
     *                  of the model. setting learning to 'off' might be useful
     *                  for indicating separate training vs. testing sets.
     */
    public void updateBookeepingVars(SpatialLattice l, boolean learn) {
        l.iterationLearnNum += 1;
        if(learn) l.iterationLearnNum += 1;
    }
    
    /**
     * This function determines each column's overlap with the current input
     * vector. The overlap of a column is the number of synapses for that column
     * that are connected (permanence value is greater than '_synPermConnected')
     * to input bits which are turned on. Overlap values that are lower than
     * the 'stimulusThreshold' are ignored. The implementation takes advantage of
     * the SpraseBinaryMatrix class to perform this calculation efficiently.
     *  
     * @param l
     * @param inputVector   a <del>numpy</del> array of 0's and 1's that comprises the input to
                            the spatial pooler.
     * @return
     */
    public int[] calculateOverlap(SpatialLattice l, int[] inputVector) {
        int[] overlaps = new int[l.getNumColumns()];
        //l.conn
        return null;
    }
}
