package org.numenta.nupic.research;

import gnu.trove.set.hash.TIntHashSet;

import org.numenta.nupic.data.SparseBinaryMatrix;
import org.numenta.nupic.data.SparseDoubleMatrix;
import org.numenta.nupic.data.SparseMatrix;
import org.numenta.nupic.data.SparseObjectMatrix;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Lattice;

public class SpatialLattice extends Lattice {
    
    private int potentialRadius = 16;
    private double potentialPct = 0.5;
    private boolean globalInhibition = false;
    private double localAreaDensity = -1.0;
    private double numActiveColumnsPerInhArea;
    private double stimulusThreshold = 0;
    private double synPermInactiveDec = 0.01;
    private double synPermActiveInc = 0.10;
    private double synPermConnected = 0.10;
    private double synPermBelowStimulusInc = synPermConnected / 10.0;
    private double minPctOverlapDutyCycles = 0.001;
    private double minPctActiveDutyCycles = 0.001;
    private double dutyCyclePeriod = 1000;
    private double maxBoost = 10.0;
    private int spVerbosity = 0;
    private int seed;
    
    private int numInputs = 1;  //product of input dimensions
    private int numColumns = 1; //product of column dimensions
    
    //Extra parameter settings
    private double synPermMin = 0.0;
    private double synPermMax = 1.0;
    private double synPermTrimThreshold = synPermActiveInc / 2.0;
    private int updatePeriod = 50;
    private double initConnectedPct = 0.5;
    
    //Internal state
    private double version = 1.0;
    public int interationNum = 0;
    public int iterationLearnNum = 0;
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
    private SparseObjectMatrix<int[]> potentialPools;
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
    private SparseObjectMatrix<double[]> permanences;
    /**
     * Initialize a tiny random tie breaker. This is used to determine winning
     * columns where the overlaps are identical.
     */
    private SparseDoubleMatrix tieBreaker;
    /**
     * 'self._connectedSynapses' is a similar matrix to 'self._permanences'
     * (rows represent cortical columns, columns represent input bits) whose
     * entries represent whether the cortical column is connected to the input
     * bit, i.e. its permanence value is greater than 'synPermConnected'. While
     * this information is readily available from the 'self._permanence' matrix,
     * it is stored separately for efficiency purposes.
     */
    private SparseObjectMatrix<int[]> connectedSynapses;
    /** 
     * Stores the number of connected synapses for each column. This is simply
     * a sum of each row of 'self._connectedSynapses'. again, while this
     * information is readily available from 'self._connectedSynapses', it is
     * stored separately for efficiency purposes.
     */
    private int[] connectedCounts = new int[numColumns];
    /**
     * The inhibition radius determines the size of a column's local
     * neighborhood. of a column. A cortical column must overcome the overlap
     * score of columns in his neighborhood in order to become actives. This
     * radius is updated every learning round. It grows and shrinks with the
     * average number of connected synapses per column.
     */
    private int inhibitionRadius = 0;
    
    private double[] overlapDutyCycles;
    private double[] activeDutyCycles;
    private double[] minOverlapDutyCycles;
    private double[] minActiveDutyCycles;
    private double[] boostFactors;
    
    
    /**
     * Constructs a new {@code SpatialLattice}
     */
    public SpatialLattice(Parameters p) {
        super();
        
        if(p != null) {
            Parameters.apply(this, p);
        }
        
        memory = new SparseObjectMatrix<Column>(columnDimensions);
        inputMatrix = new SparseBinaryMatrix(inputDimensions);
        
        for(int i = 0;i < inputDimensions.length;i++) {
            numInputs *= inputDimensions[i];
        }
        for(int i = 0;i < columnDimensions.length;i++) {
            numColumns *= columnDimensions[i];
        }
        
        potentialPools = new SparseObjectMatrix<int[]>(new int[] { numColumns, numInputs } );
        
        permanences = new SparseObjectMatrix<double[]>(new int[] { numColumns, numInputs } );
        
        tieBreaker = new SparseDoubleMatrix(new int[] { numColumns, numInputs } );
        for(int i = 0;i < numColumns;i++) {
            for(int j = 0;j < numInputs;j++) {
                tieBreaker.set(new int[] { i, j }, 0.01 * random.nextDouble());
            }
        }
        /**
         * 'connectedSynapses' is a similar matrix to 'permanences'
         * (rows represent cortical columns, columns represent input bits) whose
         * entries represent whether the cortical column is connected to the input
         * bit, i.e. its permanence value is greater than 'synPermConnected'. While
         * this information is readily available from the 'permanences' matrix,
         * it is stored separately for efficiency purposes.
         */
        connectedSynapses = new SparseObjectMatrix<int[]>(new int[] { numColumns, numInputs } );
        
        connectedCounts = new int[numColumns];
        // Initialize the set of permanence values for each column. Ensure that
        // each column is connected to enough input bits to allow it to be
        // activated.
        for(int i = 0;i < numColumns;i++) {
            int[] potential = SpatialPooler.mapPotential(this, 0, true);
            potentialPools.set(i, potential);
            double[] perm = SpatialPooler.initPermanence(this, new TIntHashSet(potential), initConnectedPct);
            SpatialPooler.updatePermanencesForColumn(this, perm, i, true);
        }
        
        overlapDutyCycles = new double[numColumns];
        activeDutyCycles = new double[numColumns];
        minOverlapDutyCycles = new double[numColumns];
        minActiveDutyCycles = new double[numColumns];
        boostFactors = new double[numColumns];
        
        SpatialPooler.updateInhibitionRadius(this);
        
        if(getVerbosity() > 0) {
            printParameters();
        }
    }
    
    /**
     * Returns the {@link SparseMatrix} which contains the model elements
     *  
     * @return  the main memory matrix
     */
    public SparseMatrix<?> getMemory() {
        return memory;
    }
    
    /**
     * Sets the {@link SparseMatrix} which contains the model elements
     * @param matrix
     */
    public void setMemory(SparseMatrix<?> matrix) {
        this.memory = matrix;
    }
    
    /**
     * Returns the input column mapping
     */
    public SparseMatrix<?> getInputMatrix() {
        return inputMatrix;
    }
    
    /**
     * Sets the input column mapping matrix
     * @param matrix
     */
    public void setInputMatrix(SparseMatrix<?> matrix) {
        this.inputMatrix = matrix;
    }
    
    /**
     * Returns the inhibition radius
     * @return
     */
    public int getInhibitionRadius() {
        return inhibitionRadius;
    }
    
    /**
     * Sets the inhibition radius
     * @param radius
     */
    public void setInhibitionRadius(int radius) {
        this.inhibitionRadius = radius;
    }
    
    /**
     * Returns the product of the input dimensions 
     * @return  the product of the input dimensions 
     */
    public int getNumInputs() {
        return numInputs;
    }
    
    /**
     * Returns the product of the column dimensions 
     * @return  the product of the column dimensions 
     */
    public int getNumColumns() {
        return numColumns;
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
     * @return  the configured potential radius
     * @see {@link #setPotentialRadius(int)}
     */
    public int getPotentialRadius() {
        return Math.min(numInputs, potentialRadius);
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
     * Returns the {@link SparseObjectMatrix} representing the 
     * proximal dendrite permanence values.
     * 
     * @return  the {@link SparseDoubleMatrix}
     */
    public SparseObjectMatrix<double[]> getPermanences() {
        return permanences;
    }
    
    /**
     * Sets the {@link SparseObjectMatrix} which represents the 
     * proximal dendrite permanence values.
     * 
     * @param s the {@link SparseDoubleMatrix}
     */
    public void setPermanences(SparseObjectMatrix<double[]> s) {
        this.permanences = s;
    }
    
    /**
     * Returns the {@link SparseObjectMatrix} that represents the connected synapses.
     * @return
     */
    public SparseObjectMatrix<int[]> getConnectedSynapses() {
        return connectedSynapses;
    }
    
    /**
     * Sets the {@link SparseObjectMatrix} representing the connected synapses.
     * @param s
     */
    public void setConnectedSysnapses(SparseObjectMatrix<int[]> s) {
        this.connectedSynapses = s;
    }
    
    /**
     * Returns the indexed count of connected synapses per column.
     * @return
     */
    public int[] getConnectedCounts() {
        return connectedCounts;
    }
    
    /**
     * Sets the indexed count of synapses connected at the columns in each index.
     * @param counts
     */
    public void setConnectedCounts(int[] counts) {
        this.connectedCounts = counts;
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
     * @return  the configured global inhibition flag
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
     * @return  the configured local area density
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
     * @return  the configured number of active columns per
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
     * @return  the stimulus threshold
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
     * @return  the synaptic permanence inactive decrement.
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
     * Returns the stimulus increment for synapse permanences below 
     * the measured threshold.
     * 
     * @return
     */
    public double getSynPermBelowStimulusInc() {
        return synPermBelowStimulusInc;
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
    public void setMinPctOverlapDutyCycles(double minPctOverlapDutyCycle) {
        this.minPctOverlapDutyCycles = minPctOverlapDutyCycle;
    }
    
    /**
     * {@see #setMinPctOverlapDutyCycles(double)}
     * @return
     */
    public double getMinPctOverlapDutyCycles() {
        return minPctOverlapDutyCycles;
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
    public void setMinPctActiveDutyCycles(double minPctActiveDutyCycle) {
        this.minPctActiveDutyCycles = minPctActiveDutyCycle;
    }
    
    /**
     * Returns the minPctActiveDutyCycle
     * @return  the minPctActiveDutyCycle
     * @see {@link #setMinPctActiveDutyCycle(double)}
     */
    public double getMinPctActiveDutyCycles() {
        return minPctActiveDutyCycles;
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
     * @return  the configured duty cycle period
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
     * @return  the max boost
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
     * @return  the verbosity setting.
     * @see {@link #setSpVerbosity(int)}
     */
    public int getSpVerbosity() {
        return spVerbosity;
    }

    /**
     * Sets the synPermTrimThreshold
     * @param threshold
     */
    public void setSynPermTrimThreshold(double threshold) {
        this.synPermTrimThreshold = threshold;
    }
    
    /**
     * Returns the synPermTrimThreshold
     * @return
     */
    public double getSynPermTrimThreshold() {
        return synPermTrimThreshold;
    }
    
    /**
     * Returns the {@link SparseObjectMatrix} which holds the mapping
     * of column indexes to their lists of potential inputs.
     * @return
     */
    public SparseObjectMatrix<int[]> getPotentialPools() {
        return this.potentialPools;
    }
    
    /**
     * 
     * @return
     */
    public double getSynPermMin() {
        return synPermMin;
    }
    
    /**
     * 
     * @return
     */
    public double getSynPermMax() {
        return synPermMax;
    }
    
    /**
     * Returns the output setting for verbosity
     * @return
     */
    public int getVerbosity() {
        return spVerbosity;
    }
    
    /**
     * Returns the version number
     * @return
     */
    public double getVersion() {
        return version;
    }
    
    /**
     * High verbose output useful for debugging
     */
    public void printParameters() {
        System.out.println("------------J  SpatialPooler Parameters ------------------");
        System.out.println("numInputs                  = " + getNumInputs());
        System.out.println("numColumns                 = " + getNumColumns());
        System.out.println("columnDimensions           = " + getColumnDimensions());
        System.out.println("numActiveColumnsPerInhArea = " + getNumActiveColumnsPerInhArea());
        System.out.println("potentialPct               = " + getPotentialPct());
        System.out.println("globalInhibition           = " + getGlobalInhibition());
        System.out.println("localAreaDensity           = " + getLocalAreaDensity());
        System.out.println("stimulusThreshold          = " + getStimulusThreshold());
        System.out.println("synPermActiveInc           = " + getSynPermActiveInc());
        System.out.println("synPermInactiveDec         = " + getSynPermInactiveDec());
        System.out.println("synPermConnected           = " + getSynPermConnected());
        System.out.println("minPctOverlapDutyCycle     = " + getMinPctOverlapDutyCycles());
        System.out.println("minPctActiveDutyCycle      = " + getMinPctActiveDutyCycles());
        System.out.println("dutyCyclePeriod            = " + getDutyCyclePeriod());
        System.out.println("maxBoost                   = " + getMaxBoost());
        System.out.println("spVerbosity                = " + getSpVerbosity());
        System.out.println("version                    = " + getVersion());
    }
}
