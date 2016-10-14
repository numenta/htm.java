package org.numenta.nupic.model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.network.Persistence;
import org.numenta.nupic.network.PersistenceAPI;
import org.numenta.nupic.serialize.SerialConfig;
import org.numenta.nupic.util.AbstractSparseBinaryMatrix;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.FlatMatrix;
import org.numenta.nupic.util.SparseMatrix;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Topology;
import org.numenta.nupic.util.UniversalRandom;

import chaschev.lang.Pair;
import gnu.trove.list.array.TIntArrayList;

/**
 * Contains the definition of the interconnected structural state of the {@link SpatialPooler} and
 * {@link TemporalMemory} as well as the state of all support structures
 * (i.e. Cells, Columns, Segments, Synapses etc.).
 *
 * In the separation of data from logic, this class represents the data/state.
 */
public class Connections implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private static final double EPSILON = 0.00001;
    
    /////////////////////////////////////// Spatial Pooler Vars ///////////////////////////////////////////
    /** <b>WARNING:</b> potentialRadius **must** be set to 
     * the inputWidth if using "globalInhibition" and if not 
     * using the Network API (which sets this automatically) 
     */
    private int potentialRadius = 16;
    private double potentialPct = 0.5;
    private boolean globalInhibition = false;
    private double localAreaDensity = -1.0;
    private double numActiveColumnsPerInhArea;
    private double stimulusThreshold = 0;
    private double synPermInactiveDec = 0.008;
    private double synPermActiveInc = 0.05;
    private double synPermConnected = 0.10;
    private double synPermBelowStimulusInc = synPermConnected / 10.0;
    private double minPctOverlapDutyCycles = 0.001;
    private double minPctActiveDutyCycles = 0.001;
    private double predictedSegmentDecrement = 0.0;
    private int dutyCyclePeriod = 1000;
    private double maxBoost = 10.0;
    private boolean wrapAround = true;
    
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
    public int spIterationNum = 0;
    public int spIterationLearnNum = 0;
    public long tmIteration = 0;
    
    public double[] boostedOverlaps;
    public int[] overlaps;
    
    /** Manages input neighborhood transformations */
    private Topology inputTopology;
    /** Manages column neighborhood transformations */
    private Topology columnTopology;
    /** A matrix representing the shape of the input. */
    protected SparseMatrix<?> inputMatrix;
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
     * the potentialPool matrix is stored using the SparseObjectMatrix
     * class, to reduce memory footprint and computation time of algorithms that
     * require iterating over the data structure.
     */
    private FlatMatrix<Pool> potentialPools;
    /**
     * Initialize a tiny random tie breaker. This is used to determine winning
     * columns where the overlaps are identical.
     */
    private double[] tieBreaker;
    /**
     * Stores the number of connected synapses for each column. This is simply
     * a sum of each row of 'connectedSynapses'. again, while this
     * information is readily available from 'connectedSynapses', it is
     * stored separately for efficiency purposes.
     */
    private AbstractSparseBinaryMatrix connectedCounts;
    /**
     * The inhibition radius determines the size of a column's local
     * neighborhood. of a column. A cortical column must overcome the overlap
     * score of columns in its neighborhood in order to become actives. This
     * radius is updated every learning round. It grows and shrinks with the
     * average number of connected synapses per column.
     */
    private int inhibitionRadius = 0;

    private double[] overlapDutyCycles;
    private double[] activeDutyCycles;
    private volatile double[] minOverlapDutyCycles;
    private volatile double[] minActiveDutyCycles;
    private double[] boostFactors;

    /////////////////////////////////////// Temporal Memory Vars ///////////////////////////////////////////

    protected Set<Cell> activeCells = new LinkedHashSet<Cell>();
    protected Set<Cell> winnerCells = new LinkedHashSet<Cell>();
    protected Set<Cell> predictiveCells = new LinkedHashSet<>();
    protected List<DistalDendrite> activeSegments = new ArrayList<>();
    protected List<DistalDendrite> matchingSegments = new ArrayList<>();
    
    /** Total number of columns */
    protected int[] columnDimensions = new int[] { 2048 };
    /** Total number of cells per column */
    protected int cellsPerColumn = 32;
    /** What will comprise the Layer input. Input (i.e. from encoder) */
    protected int[] inputDimensions = new int[] { 32, 32 };
    /**
     * If the number of active connected synapses on a segment
     * is at least this threshold, the segment is said to be active.
     */
    private int activationThreshold = 13;
    /**
     * Radius around cell from which it can
     * sample to form distal {@link DistalDendrite} connections.
     */
    private int learningRadius = 2048;
    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     */
    private int minThreshold = 10;
    /** The maximum number of synapses added to a segment during learning. */
    private int maxNewSynapseCount = 20;
    /** The maximum number of segments (distal dendrites) allowed on a cell */
    private int maxSegmentsPerCell = 255;
    /** The maximum number of synapses allowed on a given segment (distal dendrite) */
    private int maxSynapsesPerSegment = 255;
    /** Initial permanence of a new synapse */
    private double initialPermanence = 0.21;
    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     */
    private double connectedPermanence = 0.50;
    /**
     * Amount by which permanences of synapses
     * are incremented during learning.
     */
    private double permanenceIncrement = 0.10;
    /**
     * Amount by which permanences of synapses
     * are decremented during learning.
     */
    private double permanenceDecrement = 0.10;

    /** The main data structure containing columns, cells, and synapses */
    private SparseObjectMatrix<Column> memory;

    private Cell[] cells;

    ///////////////////////   Structural Elements /////////////////////////
    /** Reverse mapping from source cell to {@link Synapse} */
    public Map<Cell, LinkedHashSet<Synapse>> receptorSynapses;

    protected Map<Cell, List<DistalDendrite>> segments;
    public Map<Segment, List<Synapse>> distalSynapses;
    protected Map<Segment, List<Synapse>> proximalSynapses;

    /** Helps index each new proximal Synapse */
    protected int proximalSynapseCounter = -1;
    /** Global tracker of the next available segment index */
    protected int nextFlatIdx;
    /** Global counter incremented for each DD segment creation*/
    protected int nextSegmentOrdinal;
    /** Global counter incremented for each DD synapse creation*/
    protected int nextSynapseOrdinal;
    /** Total number of synapses */
    protected long numSynapses;
    /** Used for recycling {@link DistalDendrite} indexes */
    protected TIntArrayList freeFlatIdxs = new TIntArrayList();
    /** Indexed segments by their global index (can contain nulls) */
    protected List<DistalDendrite> segmentForFlatIdx = new ArrayList<>();
    /** Stores each cycle's most recent activity */
    public Activity lastActivity;
    /** The default random number seed */
    protected int seed = 42;
    /** The random number generator */
    public Random random = new UniversalRandom(seed);
    
    /** Sorting Lambda used for sorting active and matching segments */
    public Comparator<DistalDendrite> segmentPositionSortKey = (Comparator<DistalDendrite> & Serializable)(s1,s2) -> {
        double c1 = s1.getParentCell().getIndex() + ((double)(s1.getOrdinal() / (double)nextSegmentOrdinal));
        double c2 = s2.getParentCell().getIndex() + ((double)(s2.getOrdinal() / (double)nextSegmentOrdinal));
        return c1 == c2 ? 0 : c1 > c2 ? 1 : -1;
    };

    /** Sorting Lambda used for SpatialPooler inhibition */
    public Comparator<Pair<Integer, Double>> inhibitionComparator = (Comparator<Pair<Integer, Double>> & Serializable)
        (p1, p2) -> { 
            int p1key = p1.getFirst();
            int p2key = p2.getFirst();
            double p1val = p1.getSecond();
            double p2val = p2.getSecond();
            if(Math.abs(p2val - p1val) < 0.000000001) {
                return Math.abs(p2key - p1key) < 0.000000001 ? 0 : p2key > p1key ? -1 : 1;
            } else {
                return p2val > p1val ? -1 : 1;
            }
        };
    
    ////////////////////////////////////////
    //       Connections Constructor      //
    ////////////////////////////////////////
    /**
     * Constructs a new {@code OldConnections} object. This object
     * is usually configured via the {@link Parameters#apply(Object)}
     * method.
     */
    public Connections() {}
    
    /**
     * Returns a deep copy of this {@code Connections} object.
     * @return a deep copy of this {@code Connections}
     */
    public Connections copy() {
        PersistenceAPI api = Persistence.get(new SerialConfig());
        byte[] myBytes = api.serializer().serialize(this);
        return api.serializer().deSerialize(myBytes);
    }
    
    /**
     * Sets the derived values of the {@link SpatialPooler}'s initialization.
     */
    public void doSpatialPoolerPostInit() {
        synPermBelowStimulusInc = synPermConnected / 10.0;
        synPermTrimThreshold = synPermActiveInc / 2.0;
        if(potentialRadius == -1) {
            potentialRadius = ArrayUtils.product(inputDimensions);
        }
    }
    
    /////////////////////////////////////////
    //         General Methods             //
    /////////////////////////////////////////
    /**
     * Sets the seed used for the internal random number generator.
     * If the generator has been instantiated, this method will initialize
     * a new random generator with the specified seed.
     *
     * @param seed
     */
    public void setSeed(int seed) {
        this.seed = seed;
    }

    /**
     * Returns the configured random number seed
     * @return
     */
    public int getSeed() {
        return seed;
    }

    /**
     * Returns the thread specific {@link Random} number generator.
     * @return
     */
    public Random getRandom() {
        return random;
    }

    /**
     * Sets the random number generator.
     * @param random
     */
    public void setRandom(Random random){
        this.random = random;
    }
    
    /**
     * Returns the {@link Cell} specified by the index passed in.
     * @param index     of the specified cell to return.
     * @return
     */
    public Cell getCell(int index) {
        return cells[index];
    }

    /**
     * Returns an array containing all of the {@link Cell}s.
     * @return
     */
    public Cell[] getCells() {
        return cells;
    }

    /**
     * Sets the flat array of cells
     * @param cells
     */
    public void setCells(Cell[] cells) {
        this.cells = cells;
    }

    /**
     * Returns an array containing the {@link Cell}s specified
     * by the passed in indexes.
     *
     * @param cellIndexes   indexes of the Cells to return
     * @return
     */
    public Cell[] getCells(int... cellIndexes) {
        Cell[] retVal = new Cell[cellIndexes.length];
        for(int i = 0;i < cellIndexes.length;i++) {
            retVal[i] = cells[cellIndexes[i]];
        }
        return retVal;
    }

    /**
     * Returns a {@link LinkedHashSet} containing the {@link Cell}s specified
     * by the passed in indexes.
     *
     * @param cellIndexes   indexes of the Cells to return
     * @return
     */
    public LinkedHashSet<Cell> getCellSet(int... cellIndexes) {
        LinkedHashSet<Cell> retVal = new LinkedHashSet<Cell>(cellIndexes.length);
        for(int i = 0;i < cellIndexes.length;i++) {
            retVal.add(cells[cellIndexes[i]]);
        }
        return retVal;
    }

    /**
     * Sets the matrix containing the {@link Column}s
     * @param mem
     */
    public void setMemory(SparseObjectMatrix<Column> mem) {
        this.memory = mem;
    }

    /**
     * Returns the matrix containing the {@link Column}s
     * @return
     */
    public SparseObjectMatrix<Column> getMemory() {
        return memory;
    }
    
    /**
     * Returns the {@link Topology} overseeing input 
     * neighborhoods.
     * @return 
     */
    public Topology getInputTopology() {
        return inputTopology;
    }
    
    /**
     * Sets the {@link Topology} overseeing input 
     * neighborhoods.
     * 
     * @param topology  the input Topology
     */
    public void setInputTopology(Topology topology) {
        this.inputTopology = topology;
    }
    
    /**
     * Returns the {@link Topology} overseeing {@link Column} 
     * neighborhoods.
     * @return
     */
    public Topology getColumnTopology() {
        return columnTopology;
    }
    
    /**
     * Sets the {@link Topology} overseeing {@link Column} 
     * neighborhoods.
     * 
     * @param topology  the column Topology
     */
    public void setColumnTopology(Topology topology) {
        this.columnTopology = topology;
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

    ////////////////////////////////////////
    //       SpatialPooler Methods        //
    ////////////////////////////////////////
    /**
     * Returns the configured initial connected percent.
     * @return
     */
    public double getInitConnectedPct() {
        return this.initConnectedPct;
    }

    /**
     * Returns the cycle count.
     * @return
     */
    public int getIterationNum() {
        return spIterationNum;
    }

    /**
     * Sets the iteration count.
     * @param num
     */
    public void setIterationNum(int num) {
        this.spIterationNum = num;
    }

    /**
     * Returns the period count which is the number of cycles
     * between meta information updates.
     * @return
     */
    public int getUpdatePeriod() {
        return updatePeriod;
    }

    /**
     * Sets the update period
     * @param period
     */
    public void setUpdatePeriod(int period) {
        this.updatePeriod = period;
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
     * Sets the product of the input dimensions to
     * establish a flat count of bits in the input field.
     * @param n
     */
    public void setNumInputs(int n) {
        this.numInputs = n;
    }

    /**
     * Returns the product of the column dimensions
     * @return  the product of the column dimensions
     */
    public int getNumColumns() {
        return numColumns;
    }

    /**
     * Sets the product of the column dimensions to be
     * the column count.
     * @param n
     */
    public void setNumColumns(int n) {
        this.numColumns = n;
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
     * <b>WARNING:</b> potentialRadius **must** be set to 
     * the inputWidth if using "globalInhibition" and if not 
     * using the Network API (which sets this automatically) 
     *
     *
     * @param potentialRadius
     */
    public void setPotentialRadius(int potentialRadius) {
        this.potentialRadius = potentialRadius;
    }

    /**
     * Returns the configured potential radius
     * 
     * @return  the configured potential radius
     * @see setPotentialRadius
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
     * @see setPotentialPct
     */
    public double getPotentialPct() {
        return potentialPct;
    }

    /**
     * Sets the {@link SparseObjectMatrix} which represents the
     * proximal dendrite permanence values.
     *
     * @param s the {@link SparseObjectMatrix}
     */
    public void setProximalPermanences(SparseObjectMatrix<double[]> s) {
        for(int idx : s.getSparseIndices()) {
            memory.getObject(idx).setProximalPermanences(this, s.getObject(idx));
        }
    }

    /**
     * Returns the count of {@link Synapse}s on
     * {@link ProximalDendrite}s
     * @return
     */
    public int getProximalSynapseCount() {
        return proximalSynapseCounter + 1;
    }
    
    /**
     * Sets the count of {@link Synapse}s on
     * {@link ProximalDendrite}s
     * @param i
     */
    public void setProximalSynapseCount(int i) {
        this.proximalSynapseCounter = i;
    }
    
    /**
     * Increments and returns the incremented
     * proximal {@link Synapse} count.
     *
     * @return
     */
    public int incrementProximalSynapses() {
        return ++proximalSynapseCounter;
    }

    /**
     * Decrements and returns the decremented
     * proximal {link Synapse} count
     * @return
     */
    public int decrementProximalSynapses() {
        return --proximalSynapseCounter;
    }
    
    /**
     * Returns the indexed count of connected synapses per column.
     * @return
     */
    public AbstractSparseBinaryMatrix getConnectedCounts() {
        return connectedCounts;
    }

    /**
     * Returns the connected count for the specified column.
     * @param columnIndex
     * @return
     */
    public int getConnectedCount(int columnIndex) {
        return connectedCounts.getTrueCount(columnIndex);
    }

    /**
     * Sets the indexed count of synapses connected at the columns in each index.
     * @param counts
     */
    public void setConnectedCounts(int[] counts) {
        for(int i = 0;i < counts.length;i++) {
            connectedCounts.setTrueCount(i, counts[i]);
        }
    }

    /**
     * Sets the connected count {@link AbstractSparseBinaryMatrix}
     * @param columnIndex
     * @param count
     */
    public void setConnectedMatrix(AbstractSparseBinaryMatrix matrix) {
        this.connectedCounts = matrix;
    }

    /**
     * Sets the array holding the random noise added to proximal dendrite overlaps.
     *
     * @param tieBreaker	random values to help break ties
     */
    public void setTieBreaker(double[] tieBreaker) {
        this.tieBreaker = tieBreaker;
    }

    /**
     * Returns the array holding random values used to add to overlap scores
     * to break ties.
     *
     * @return
     */
    public double[] getTieBreaker() {
        return tieBreaker;
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
     *
     * @see setGlobalInhibition
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
     * @see setLocalAreaDensity
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
     * @see setNumActiveColumnsPerInhArea
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
     * @see setStimulusThreshold
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
     * @see setSynPermInactiveDec
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
     * @see setSynPermActiveInc
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
     * @param synPermConnected
     */
    public void setSynPermConnected(double synPermConnected) {
        this.synPermConnected = synPermConnected;
    }

    /**
     * Returns the synapse permanence connected threshold
     * @return the synapse permanence connected threshold
     * @see setSynPermConnected
     */
    public double getSynPermConnected() {
        return synPermConnected;
    }

    /**
     * Sets the stimulus increment for synapse permanences below
     * the measured threshold.
     * @param stim
     */
    public void setSynPermBelowStimulusInc(double stim) {
        this.synPermBelowStimulusInc = stim;
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
     * see {@link #setMinPctOverlapDutyCycles(double)}
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
     * see {@link #setMinPctActiveDutyCycles(double)}
     * @return  the minPctActiveDutyCycle
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
    public void setDutyCyclePeriod(int dutyCyclePeriod) {
        this.dutyCyclePeriod = dutyCyclePeriod;
    }

    /**
     * Returns the configured duty cycle period
     * see {@link #setDutyCyclePeriod(double)}
     * @return  the configured duty cycle period
     */
    public int getDutyCyclePeriod() {
        return dutyCyclePeriod;
    }

    /**
     * The maximum overlap boost factor. Each column's
     * overlap gets multiplied by a boost factor
     * before it gets considered for inhibition.
     * The actual boost factor for a column is number
     * between 1.0 and maxBoost. A boost factor of 1.0 is
     * used if the duty cycle is &gt;= minOverlapDutyCycle,
     * maxBoost is used if the duty cycle is 0, and any duty
     * cycle in between is linearly extrapolated from these
     * 2 end points.
     *
     * @param maxBoost
     */
    public void setMaxBoost(double maxBoost) {
        this.maxBoost = maxBoost;
    }

    /**
     * Returns the max boost
     * see {@link #setMaxBoost(double)}
     * @return  the max boost
     */
    public double getMaxBoost() {
        return maxBoost;
    }
    
    /**
     * Specifies whether neighborhoods wider than the 
     * borders wrap around to the other side.
     * @param b
     */
    public void setWrapAround(boolean b) {
        this.wrapAround = b;
    }
    
    /**
     * Returns a flag indicating whether neighborhoods
     * wider than the borders, wrap around to the other
     * side.
     * @return
     */
    public boolean isWrapAround() {
        return wrapAround;
    }
    
    /**
     * Sets and Returns the boosted overlap score for each column
     * @param boostedOverlaps
     * @return
     */
    public double[] setBoostedOverlaps(double[] boostedOverlaps) {
        return this.boostedOverlaps = boostedOverlaps;
    }
   
    /**
     * Returns the boosted overlap score for each column
     * @return the boosted overlaps
     */
    public double[] getBoostedOverlaps() {
        return boostedOverlaps;
    }
    
    /**
     * Sets and Returns the overlap score for each column
     * @param overlaps
     * @return
     */
    public int[] setOverlaps(int[] overlaps) {
        return this.overlaps = overlaps;
    }
   
    /**
     * Returns the overlap score for each column
     * @return the overlaps
     */
    public int[] getOverlaps() {
        return overlaps;
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
     * Sets the {@link FlatMatrix} which holds the mapping
     * of column indexes to their lists of potential inputs.
     *
     * @param pools		{@link FlatMatrix} which holds the pools.
     */
    public void setPotentialPools(FlatMatrix<Pool>   pools) {
        this.potentialPools = pools;
    }

    /**
     * Returns the {@link FlatMatrix} which holds the mapping
     * of column indexes to their lists of potential inputs.
     * @return	the potential pools
     */
    public FlatMatrix<Pool> getPotentialPools() {
        return this.potentialPools;
    }

    /**
     * Returns the minimum {@link Synapse} permanence.
     * @return
     */
    public double getSynPermMin() {
        return synPermMin;
    }

    /**
     * Returns the maximum {@link Synapse} permanence.
     * @return
     */
    public double getSynPermMax() {
        return synPermMax;
    }

    /**
     * Returns the version number
     * @return
     */
    public double getVersion() {
        return version;
    }

    /**
     * Returns the overlap duty cycles.
     * @return
     */
    public double[] getOverlapDutyCycles() {
        return overlapDutyCycles;
    }

    /**
     * Sets the overlap duty cycles
     * @param overlapDutyCycles
     */
    public void setOverlapDutyCycles(double[] overlapDutyCycles) {
        this.overlapDutyCycles = overlapDutyCycles;
    }

    /**
     * Returns the dense (size=numColumns) array of duty cycle stats.
     * @return	the dense array of active duty cycle values.
     */
    public double[] getActiveDutyCycles() {
        return activeDutyCycles;
    }

    /**
     * Sets the dense (size=numColumns) array of duty cycle stats.
     * @param activeDutyCycles
     */
    public void setActiveDutyCycles(double[] activeDutyCycles) {
        this.activeDutyCycles = activeDutyCycles;
    }

    /**
     * Applies the dense array values which aren't -1 to the array containing
     * the active duty cycles of the column corresponding to the index specified.
     * The length of the specified array must be as long as the configured number
     * of columns of this {@code OldConnections}' column configuration.
     *
     * @param	denseActiveDutyCycles	a dense array containing values to set.
     */
    public void updateActiveDutyCycles(double[] denseActiveDutyCycles) {
        for(int i = 0;i < denseActiveDutyCycles.length;i++) {
            if(denseActiveDutyCycles[i] != -1) {
                activeDutyCycles[i] = denseActiveDutyCycles[i];
            }
        }
    }

    /**
     * Returns the minOverlapDutyCycles.
     * @return	the minOverlapDutyCycles.
     */
    public double[] getMinOverlapDutyCycles() {
        return minOverlapDutyCycles;
    }

    /**
     * Sets the minOverlapDutyCycles
     * @param minOverlapDutyCycles	the minOverlapDutyCycles
     */
    public void setMinOverlapDutyCycles(double[] minOverlapDutyCycles) {
        this.minOverlapDutyCycles = minOverlapDutyCycles;
    }

    /**
     * Returns the minActiveDutyCycles
     * @return	the minActiveDutyCycles
     */
    public double[] getMinActiveDutyCycles() {
        return minActiveDutyCycles;
    }

    /**
     * Sets the minActiveDutyCycles
     * @param minActiveDutyCycles	the minActiveDutyCycles
     */
    public void setMinActiveDutyCycles(double[] minActiveDutyCycles) {
        this.minActiveDutyCycles = minActiveDutyCycles;
    }

    /**
     * Returns the array of boost factors
     * @return	the array of boost factors
     */
    public double[] getBoostFactors() {
        return boostFactors;
    }

    /**
     * Sets the array of boost factors
     * @param boostFactors	the array of boost factors
     */
    public void setBoostFactors(double[] boostFactors) {
        this.boostFactors = boostFactors;
    }
    
    
	////////////////////////////////////////
	//       TemporalMemory Methods       //
	////////////////////////////////////////
    
    /**
     * Return type from {@link Connections#computeActivity(Set, double, int, double, int, boolean)}
     */
    public static class Activity implements Serializable {
    	/** default serial */
        private static final long serialVersionUID = 1L;
        
        public int[] numActiveConnected;
        public int[] numActivePotential;
        
        public Activity(int[] numConnected, int[] numPotential) {
            this.numActiveConnected = numConnected;
            this.numActivePotential = numPotential;
        }
    }
    
    /**
     * Compute each segment's number of active synapses for a given input.
     * In the returned lists, a segment's active synapse count is stored at index
     * `segment.flatIdx`.
     * 
     * @param activePresynapticCells
     * @param connectedPermanence
     * @return
     */
    public Activity computeActivity(Collection<Cell> activePresynapticCells, double connectedPermanence) {
        int[] numActiveConnectedSynapsesForSegment = new int[nextFlatIdx];
        int[] numActivePotentialSynapsesForSegment = new int[nextFlatIdx];
        
        double threshold = connectedPermanence - EPSILON;
        
        for(Cell cell : activePresynapticCells) {
            for(Synapse synapse : getReceptorSynapses(cell)) {
                int flatIdx = synapse.getSegment().getIndex();
                ++numActivePotentialSynapsesForSegment[flatIdx];
                if(synapse.getPermanence() > threshold) {
                    ++numActiveConnectedSynapsesForSegment[flatIdx];
                }
            }
        }
        
    	return lastActivity = new Activity(
    	    numActiveConnectedSynapsesForSegment, 
    	        numActivePotentialSynapsesForSegment);
    }
    
    /**
     * Returns the last {@link Activity} computed during the most
     * recently executed cycle.
     * 
     * @return  the last activity to be computed.
     */
    public Activity getLastActivity() {
        return lastActivity;
    }
    
    /**
     * Record the fact that a segment had some activity. This information is
     * used during segment cleanup.
     * 
     * @param segment		the segment for which to record activity
     */
    public void recordSegmentActivity(DistalDendrite segment) {
    	segment.setLastUsedIteration(tmIteration);
    }
    
    /**
     * Mark the passage of time. This information is used during segment
     * cleanup.
     */
    public void startNewIteration() {
    	++tmIteration;
    }
    
    
	/////////////////////////////////////////////////////////////////
	//     Segment (Specifically, Distal Dendrite) Operations      //
	/////////////////////////////////////////////////////////////////
    
    /**
     * Adds a new {@link DistalDendrite} segment on the specified {@link Cell},
     * or reuses an existing one.
     * 
     * @param cell  the Cell to which a segment is added.
     * @return  the newly created segment or a reused segment
     */
    public DistalDendrite createSegment(Cell cell) {
    	while(numSegments(cell) >= maxSegmentsPerCell) {
            destroySegment(leastRecentlyUsedSegment(cell));
        }
    	
    	int flatIdx;
    	int len;
    	if((len = freeFlatIdxs.size()) > 0) {
    		flatIdx = freeFlatIdxs.get(len - 1);
    		freeFlatIdxs.remove(len - 1, 1);
    	}else{
    		flatIdx = nextFlatIdx;
    		segmentForFlatIdx.add(null);
    		++nextFlatIdx;
    	}
    	
    	int ordinal = nextSegmentOrdinal;
    	++nextSegmentOrdinal;
    	
    	DistalDendrite segment = new DistalDendrite(cell, flatIdx, tmIteration, ordinal);
    	getSegments(cell, true).add(segment);
    	segmentForFlatIdx.set(flatIdx, segment);
    	
    	return segment;
    }
    
    /**
     * Destroys a segment ({@link DistalDendrite})
     * @param segment   the segment to destroy
     */
    public void destroySegment(DistalDendrite segment) {
    	// Remove the synapses from all data structures outside this Segment.
    	List<Synapse> synapses = getSynapses(segment);
    	int len = synapses.size();
    	getSynapses(segment).stream().forEach(s -> removeSynapseFromPresynapticMap(s));
    	numSynapses -= len;
    	
    	// Remove the segment from the cell's list.
    	getSegments(segment.getParentCell()).remove(segment);
    	
    	// Remove the segment from the map
    	distalSynapses.remove(segment);
    	
    	// Free the flatIdx and remove the final reference so the Segment can be
        // garbage-collected.
    	freeFlatIdxs.add(segment.getIndex());
    	segmentForFlatIdx.set(segment.getIndex(), null);
    }
    
    /**
     * Used internally to return the least recently activated segment on 
     * the specified cell
     * 
     * @param cell  cell to search for segments on
     * @return  the least recently activated segment on 
     *          the specified cell
     */
    private DistalDendrite leastRecentlyUsedSegment(Cell cell) {
        List<DistalDendrite> segments = getSegments(cell, false);
        DistalDendrite minSegment = null;
        long minIteration = Long.MAX_VALUE;
        
        for(DistalDendrite dd : segments) {
            if(dd.lastUsedIteration() < minIteration) {
            	minSegment = dd;
                minIteration = dd.lastUsedIteration();
            }
        }
        
        return minSegment;
    }
    
    /**
     * Returns the total number of {@link DistalDendrite}s
     * 
     * @return  the total number of segments
     */
    public int numSegments() {
        return numSegments(null);
    }
    
    /**
     * Returns the number of {@link DistalDendrite}s on a given {@link Cell}
     * if specified, or the total number if the "optionalCellArg" is null.
     * 
     * @param optionalCellArg   an optional Cell to specify the context of the segment count.
     * @return  either the total number of segments or the number on a specified cell.
     */
    public int numSegments(Cell optionalCellArg) {
        if(optionalCellArg != null) {
            return getSegments(optionalCellArg).size();
        }
        
        return nextFlatIdx - freeFlatIdxs.size();
    }
    
    /**
     * Returns the mapping of {@link Cell}s to their {@link DistalDendrite}s.
     *
     * @param cell      the {@link Cell} used as a key.
     * @return          the mapping of {@link Cell}s to their {@link DistalDendrite}s.
     */
    public List<DistalDendrite> getSegments(Cell cell) {
        return getSegments(cell, false);
    }

    /**
     * Returns the mapping of {@link Cell}s to their {@link DistalDendrite}s.
     *
     * @param cell              the {@link Cell} used as a key.
     * @param doLazyCreate      create a container for future use if true, if false
     *                          return an orphaned empty set.
     * @return          the mapping of {@link Cell}s to their {@link DistalDendrite}s.
     */
    public List<DistalDendrite> getSegments(Cell cell, boolean doLazyCreate) {
        if(cell == null) {
            throw new IllegalArgumentException("Cell was null");
        }

        if(segments == null) {
            segments = new LinkedHashMap<Cell, List<DistalDendrite>>();
        }

        List<DistalDendrite> retVal = null;
        if((retVal = segments.get(cell)) == null) {
            if(!doLazyCreate) return Collections.emptyList();
            segments.put(cell, retVal = new ArrayList<DistalDendrite>());
        }

        return retVal;
    }
    
    /**
     * Get the segment with the specified flatIdx.
     * @param index		The segment's flattened list index.
     * @return	the {@link DistalDendrite} who's index matches.
     */
    public DistalDendrite segmentForFlatIdx(int index) {
    	return segmentForFlatIdx.get(index);
    }
    
    /**
     * Returns the index of the {@link Column} owning the cell which owns 
     * the specified segment.
     * @param segment   the {@link DistalDendrite} of the cell whose column index is desired.
     * @return  the owning column's index
     */
    public int columnIndexForSegment(DistalDendrite segment) {
        return segment.getParentCell().getIndex() / cellsPerColumn;
    }
    
    /**
     * <b>FOR TEST USE ONLY</b>
     * @return
     */
    public Map<Cell, List<DistalDendrite>> getSegmentMapping() {
        return new LinkedHashMap<>(segments);
    }
    
    /**
     * Set by the {@link TemporalMemory} following a compute cycle.
     * @param l
     */
    public void setActiveSegments(List<DistalDendrite> l) {
        this.activeSegments = l;
    }
    
    /**
     * Retrieved by the {@link TemporalMemorty} prior to a compute cycle.
     * @return
     */
    public List<DistalDendrite> getActiveSegments() {
        return activeSegments;
    }
    
    /**
     * Set by the {@link TemporalMemory} following a compute cycle.
     * @param l
     */
    public void setMatchingSegments(List<DistalDendrite> l) {
        this.matchingSegments = l;
    }
    
    /**
     * Retrieved by the {@link TemporalMemorty} prior to a compute cycle.
     * @return
     */
    public List<DistalDendrite> getMatchingSegments() {
        return matchingSegments;
    }
    
    
    /////////////////////////////////////////////////////////////////
    //                    Synapse Operations                       //
    /////////////////////////////////////////////////////////////////
    
    /**
     * Creates a new synapse on a segment.
     * 
     * @param segment               the {@link DistalDendrite} segment to which a {@link Synapse} is 
     *                              being created
     * @param presynapticCell       the source {@link Cell}
     * @param permanence            the initial permanence
     * @return  the created {@link Synapse}
     */
    public Synapse createSynapse(DistalDendrite segment, Cell presynapticCell, double permanence) {
        while(numSynapses(segment) >= maxSynapsesPerSegment) {
            destroySynapse(minPermanenceSynapse(segment));
        }
        
        Synapse synapse = null;
	    getSynapses(segment).add(
	        synapse = new Synapse(
	            presynapticCell, segment, nextSynapseOrdinal, permanence));
	    
        getReceptorSynapses(presynapticCell, true).add(synapse);
        
        ++nextSynapseOrdinal;
        
        ++numSynapses;
        
        return synapse;
    }
    
    /**
     * Destroys the specified {@link Synapse}
     * @param synapse   the Synapse to destroy
     */
    public void destroySynapse(Synapse synapse) {
        --numSynapses;
        
        removeSynapseFromPresynapticMap(synapse);
        
        getSynapses((DistalDendrite)synapse.getSegment()).remove(synapse);
    }
    
    /**
     * Removes the specified {@link Synapse} from its
     * pre-synaptic {@link Cell}'s map of synapses it 
     * activates.
     * 
     * @param synapse   the synapse to remove
     */
    public void removeSynapseFromPresynapticMap(Synapse synapse) {
    	Set<Synapse> presynapticSynapses;
        Cell cell = synapse.getPresynapticCell();
        (presynapticSynapses = getReceptorSynapses(cell, false)).remove(synapse);
        
        if(presynapticSynapses.isEmpty()) {
            receptorSynapses.remove(cell);
        }
    }
    
    /**
     * Used internally to find the synapse with the smallest permanence
     * on the given segment.
     * 
     * @param dd    Segment object to search for synapses on
     * @return  Synapse object on the segment with the minimal permanence
     */
    private Synapse minPermanenceSynapse(DistalDendrite dd) {
        List<Synapse> synapses = getSynapses(dd).stream().sorted().collect(Collectors.toList());
        Synapse min = null;
        double minPermanence = Double.MAX_VALUE;
        
        for(Synapse synapse : synapses) {
            if(!synapse.destroyed() && synapse.getPermanence() < minPermanence - EPSILON) {
                min = synapse;
                minPermanence = synapse.getPermanence();
            }
        }
        
        return min;
    }
    
    /**
     * Returns the total number of {@link Synapse}s
     * 
     * @return  either the total number of synapses
     */
    public long numSynapses() {
        return numSynapses(null);
    }
    
    /**
     * Returns the number of {@link Synapse}s on a given {@link DistalDendrite}
     * if specified, or the total number if the "optionalSegmentArg" is null.
     * 
     * @param optionalSegmentArg    an optional Segment to specify the context of the synapse count.
     * @return  either the total number of synapses or the number on a specified segment.
     */
    public long numSynapses(DistalDendrite optionalSegmentArg) {
        if(optionalSegmentArg != null) {
            return getSynapses(optionalSegmentArg).size();
        }
        
        return numSynapses;
    }
    
    /**
     * Returns the mapping of {@link Cell}s to their reverse mapped
     * {@link Synapse}s.
     *
     * @param cell      the {@link Cell} used as a key.
     * @return          the mapping of {@link Cell}s to their reverse mapped
     *                  {@link Synapse}s.
     */
    public Set<Synapse> getReceptorSynapses(Cell cell) {
        return getReceptorSynapses(cell, false);
    }

    /**
     * Returns the mapping of {@link Cell}s to their reverse mapped
     * {@link Synapse}s.
     *
     * @param cell              the {@link Cell} used as a key.
     * @param doLazyCreate      create a container for future use if true, if false
     *                          return an orphaned empty set.
     * @return          the mapping of {@link Cell}s to their reverse mapped
     *                  {@link Synapse}s.
     */
    public Set<Synapse> getReceptorSynapses(Cell cell, boolean doLazyCreate) {
        if(cell == null) {
            throw new IllegalArgumentException("Cell was null");
        }

        if(receptorSynapses == null) {
            receptorSynapses = new LinkedHashMap<>();
        }

        LinkedHashSet<Synapse> retVal = null;
        if((retVal = receptorSynapses.get(cell)) == null) {
            if(!doLazyCreate) return Collections.emptySet();
            receptorSynapses.put(cell, retVal = new LinkedHashSet<>());
        }

        return retVal;
    }
    
    /**
     * Returns the mapping of {@link DistalDendrite}s to their {@link Synapse}s.
     *
     * @param segment   the {@link DistalDendrite} used as a key.
     * @return          the mapping of {@link DistalDendrite}s to their {@link Synapse}s.
     */
    public List<Synapse> getSynapses(DistalDendrite segment) {
        if(segment == null) {
            throw new IllegalArgumentException("Segment was null");
        }

        if(distalSynapses == null) {
            distalSynapses = new LinkedHashMap<Segment, List<Synapse>>();
        }

        List<Synapse> retVal = null;
        if((retVal = distalSynapses.get(segment)) == null) {
            distalSynapses.put(segment, retVal = new ArrayList<Synapse>());
        }

        return retVal;
    }

    /**
     * Returns the mapping of {@link ProximalDendrite}s to their {@link Synapse}s.
     *
     * @param segment   the {@link ProximalDendrite} used as a key.
     * @return          the mapping of {@link ProximalDendrite}s to their {@link Synapse}s.
     */
    public List<Synapse> getSynapses(ProximalDendrite segment) {
        if(segment == null) {
            throw new IllegalArgumentException("Segment was null");
        }

        if(proximalSynapses == null) {
            proximalSynapses = new LinkedHashMap<Segment, List<Synapse>>();
        }

        List<Synapse> retVal = null;
        if((retVal = proximalSynapses.get(segment)) == null) {
            proximalSynapses.put(segment, retVal = new ArrayList<Synapse>());
        }

        return retVal;
    }
    
    /**
     * <b>FOR TEST USE ONLY<b>
     * @return
     */
    public Map<Cell, HashSet<Synapse>> getReceptorSynapseMapping() {
        return new LinkedHashMap<>(receptorSynapses);
    }

    /**
     * Clears all {@link TemporalMemory} state.
     */
    public void clear() {
        activeCells.clear();
        winnerCells.clear();
        predictiveCells.clear();
    }

    /**
     * Returns the current {@link Set} of active {@link Cell}s
     *
     * @return  the current {@link Set} of active {@link Cell}s
     */
    public Set<Cell> getActiveCells() {
        return activeCells;
    }

    /**
     * Sets the current {@link Set} of active {@link Cell}s
     * @param cells
     */
    public void setActiveCells(Set<Cell> cells) {
        this.activeCells = cells;
    }

    /**
     * Returns the current {@link Set} of winner cells
     *
     * @return  the current {@link Set} of winner cells
     */
    public Set<Cell> getWinnerCells() {
        return winnerCells;
    }

    /**
     * Sets the current {@link Set} of winner {@link Cell}s
     * @param cells
     */
    public void setWinnerCells(Set<Cell> cells) {
        this.winnerCells = cells;
    }

    /**
     * Returns the {@link Set} of predictive cells.
     * @return
     */
    public Set<Cell> getPredictiveCells() {
        if(predictiveCells.isEmpty()) {
            Cell previousCell = null;
            Cell currCell = null;
            
            List<DistalDendrite> temp = new ArrayList<>(activeSegments);
            for(DistalDendrite activeSegment : temp) {
                if((currCell = activeSegment.getParentCell()) != previousCell) {
                    predictiveCells.add(previousCell = currCell);
                }
            }
        }
        return predictiveCells;
    }
    
    /**
     * Clears the previous predictive cells from the list.
     */
    public void clearPredictiveCells() {
        this.predictiveCells.clear();
    }

    /**
     * Returns the column at the specified index.
     * @param index
     * @return
     */
    public Column getColumn(int index) {
        return memory.getObject(index);
    }

    /**
     * Sets the number of {@link Column}.
     *
     * @param columnDimensions
     */
    public void setColumnDimensions(int[] columnDimensions) {
        this.columnDimensions = columnDimensions;
    }

    /**
     * Gets the number of {@link Column}.
     *
     * @return columnDimensions
     */
    public int[] getColumnDimensions() {
        return this.columnDimensions;
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
     * see {@link #setInputDimensions(int[])}
     * @return the configured input dimensions
     */
    public int[] getInputDimensions() {
        return inputDimensions;
    }

    /**
     * Sets the number of {@link Cell}s per {@link Column}
     * @param cellsPerColumn
     */
    public void setCellsPerColumn(int cellsPerColumn) {
        this.cellsPerColumn = cellsPerColumn;
    }

    /**
     * Gets the number of {@link Cell}s per {@link Column}.
     *
     * @return cellsPerColumn
     */
    public int getCellsPerColumn() {
        return this.cellsPerColumn;
    }

    /**
     * Sets the activation threshold.
     *
     * If the number of active connected synapses on a segment
     * is at least this threshold, the segment is said to be active.
     *
     * @param activationThreshold
     */
    public void setActivationThreshold(int activationThreshold) {
        this.activationThreshold = activationThreshold;
    }

    /**
     * Returns the activation threshold.
     * @return
     */
    public int getActivationThreshold() {
        return activationThreshold;
    }

    /**
     * Radius around cell from which it can
     * sample to form distal dendrite connections.
     *
     * @param   learningRadius
     */
    public void setLearningRadius(int learningRadius) {
        this.learningRadius = learningRadius;
    }

    /**
     * Returns the learning radius.
     * @return
     */
    public int getLearningRadius() {
        return learningRadius;
    }

    /**
     * If the number of synapses active on a segment is at least this
     * threshold, it is selected as the best matching
     * cell in a bursting column.
     *
     * @param   minThreshold
     */
    public void setMinThreshold(int minThreshold) {
        this.minThreshold = minThreshold;
    }

    /**
     * Returns the minimum threshold of active synapses to be picked as best.
     * @return
     */
    public int getMinThreshold() {
        return minThreshold;
    }

    /**
     * The maximum number of synapses added to a segment during learning.
     *
     * @param   maxNewSynapseCount
     */
    public void setMaxNewSynapseCount(int maxNewSynapseCount) {
        this.maxNewSynapseCount = maxNewSynapseCount;
    }

    /**
     * Returns the maximum number of synapses added to a segment during
     * learning.
     *
     * @return
     */
    public int getMaxNewSynapseCount() {
        return maxNewSynapseCount;
    }
    
    /**
     * The maximum number of segments allowed on a given cell
     * @param maxSegmentsPerCell
     */
    public void setMaxSegmentsPerCell(int maxSegmentsPerCell) {
        this.maxSegmentsPerCell = maxSegmentsPerCell;
    }
    
    /**
     * Returns the maximum number of segments allowed on a given cell
     * @return
     */
    public int getMaxSegmentsPerCell() {
        return maxSegmentsPerCell;
    }
    
    /**
     * The maximum number of synapses allowed on a given segment
     * @param maxSynapsesPerSegment
     */
    public void setMaxSynapsesPerSegment(int maxSynapsesPerSegment) {
        this.maxSynapsesPerSegment = maxSynapsesPerSegment;
    }
    
    /**
     * Returns the maximum number of synapses allowed per segment
     * @return
     */
    public int getMaxSynapsesPerSegment() {
        return maxSynapsesPerSegment;
    }

    /**
     * Initial permanence of a new synapse
     *
     * @param   initialPermanence
     */
    public void setInitialPermanence(double initialPermanence) {
        this.initialPermanence = initialPermanence;
    }

    /**
     * Returns the initial permanence setting.
     * @return
     */
    public double getInitialPermanence() {
        return initialPermanence;
    }

    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     *
     * @param connectedPermanence
     */
    public void setConnectedPermanence(double connectedPermanence) {
        this.connectedPermanence = connectedPermanence;
    }

    /**
     * If the permanence value for a synapse
     * is greater than this value, it is said
     * to be connected.
     *
     * @return
     */
    public double getConnectedPermanence() {
        return connectedPermanence;
    }

    /**
     * Amount by which permanences of synapses
     * are incremented during learning.
     *
     * @param   permanenceIncrement
     */
    public void setPermanenceIncrement(double permanenceIncrement) {
        this.permanenceIncrement = permanenceIncrement;
    }

    /**
     * Amount by which permanences of synapses
     * are incremented during learning.
     */
    public double getPermanenceIncrement() {
        return this.permanenceIncrement;
    }

    /**
     * Amount by which permanences of synapses
     * are decremented during learning.
     *
     * @param   permanenceDecrement
     */
    public void setPermanenceDecrement(double permanenceDecrement) {
        this.permanenceDecrement = permanenceDecrement;
    }

    /**
     * Amount by which permanences of synapses
     * are decremented during learning.
     */
    public double getPermanenceDecrement() {
        return this.permanenceDecrement;
    }

    /**
     * Amount by which active permanences of synapses of previously predicted but inactive segments are decremented.
     * @param predictedSegmentDecrement
     */
    public void setPredictedSegmentDecrement(double predictedSegmentDecrement) {
        this.predictedSegmentDecrement = predictedSegmentDecrement;
    }

    /**
     * Returns the predictedSegmentDecrement amount.
     * @return
     */
    public double getPredictedSegmentDecrement() {
        return this.predictedSegmentDecrement;
    }

    /**
     * Converts a {@link Collection} of {@link Cell}s to a list
     * of cell indexes.
     *
     * @param cells
     * @return
     */
    public static List<Integer> asCellIndexes(Collection<Cell> cells) {
        List<Integer> ints = new ArrayList<Integer>();
        for(Cell cell : cells) {
            ints.add(cell.getIndex());
        }

        return ints;
    }

    /**
     * Converts a {@link Collection} of {@link Column}s to a list
     * of column indexes.
     *
     * @param columns
     * @return
     */
    public static List<Integer> asColumnIndexes(Collection<Column> columns) {
        List<Integer> ints = new ArrayList<Integer>();
        for(Column col : columns) {
            ints.add(col.getIndex());
        }

        return ints;
    }

    /**
     * Returns a list of the {@link Cell}s specified.
     * @param cells		the indexes of the {@link Cell}s to return
     * @return	the specified list of cells
     */
    public List<Cell> asCellObjects(Collection<Integer> cells) {
        List<Cell> objs = new ArrayList<Cell>();
        for(int i : cells) {
            objs.add(this.cells[i]);
        }
        return objs;
    }

    /**
     * Returns a list of the {@link Column}s specified.
     * @param cols		the indexes of the {@link Column}s to return
     * @return		the specified list of columns
     */
    public List<Column> asColumnObjects(Collection<Integer> cols) {
        List<Column> objs = new ArrayList<Column>();
        for(int i : cols) {
            objs.add(this.memory.getObject(i));
        }
        return objs;
    }

    /**
     * Returns a {@link Set} view of the {@link Column}s specified by
     * the indexes passed in.
     *
     * @param indexes		the indexes of the Columns to return
     * @return				a set view of the specified columns
     */
    public LinkedHashSet<Column> getColumnSet(int[] indexes) {
        LinkedHashSet<Column> retVal = new LinkedHashSet<Column>();
        for(int i = 0;i < indexes.length;i++) {
            retVal.add(memory.getObject(indexes[i]));
        }
        return retVal;
    }

    /**
     * Returns a {@link List} view of the {@link Column}s specified by
     * the indexes passed in.
     *
     * @param indexes		the indexes of the Columns to return
     * @return				a List view of the specified columns
     */
    public List<Column> getColumnList(int[] indexes) {
        List<Column> retVal = new ArrayList<Column>();
        for(int i = 0;i < indexes.length;i++) {
            retVal.add(memory.getObject(indexes[i]));
        }
        return retVal;
    }
    
    /**
     * High verbose output useful for debugging
     */
    public void printParameters() {
        System.out.println("------------ SpatialPooler Parameters ------------------");
        System.out.println("numInputs                  = " + getNumInputs());
        System.out.println("numColumns                 = " + getNumColumns());
        System.out.println("cellsPerColumn             = " + getCellsPerColumn());
        System.out.println("columnDimensions           = " + Arrays.toString(getColumnDimensions()));
        System.out.println("numActiveColumnsPerInhArea = " + getNumActiveColumnsPerInhArea());
        System.out.println("potentialPct               = " + getPotentialPct());
        System.out.println("potentialRadius            = " + getPotentialRadius());
        System.out.println("globalInhibition           = " + getGlobalInhibition());
        System.out.println("localAreaDensity           = " + getLocalAreaDensity());
        System.out.println("inhibitionRadius           = " + getInhibitionRadius());
        System.out.println("stimulusThreshold          = " + getStimulusThreshold());
        System.out.println("synPermActiveInc           = " + getSynPermActiveInc());
        System.out.println("synPermInactiveDec         = " + getSynPermInactiveDec());
        System.out.println("synPermConnected           = " + getSynPermConnected());
        System.out.println("minPctOverlapDutyCycle     = " + getMinPctOverlapDutyCycles());
        System.out.println("minPctActiveDutyCycle      = " + getMinPctActiveDutyCycles());
        System.out.println("dutyCyclePeriod            = " + getDutyCyclePeriod());
        System.out.println("maxBoost                   = " + getMaxBoost());
        System.out.println("version                    = " + getVersion());

        System.out.println("\n------------ TemporalMemory Parameters ------------------");
        System.out.println("activationThreshold        = " + getActivationThreshold());
        System.out.println("learningRadius             = " + getLearningRadius());
        System.out.println("minThreshold               = " + getMinThreshold());
        System.out.println("maxNewSynapseCount         = " + getMaxNewSynapseCount());
        System.out.println("maxSynapsesPerSegment      = " + getMaxSynapsesPerSegment());
        System.out.println("maxSegmentsPerCell         = " + getMaxSegmentsPerCell());
        System.out.println("initialPermanence          = " + getInitialPermanence());
        System.out.println("connectedPermanence        = " + getConnectedPermanence());
        System.out.println("permanenceIncrement        = " + getPermanenceIncrement());
        System.out.println("permanenceDecrement        = " + getPermanenceDecrement());
        System.out.println("predictedSegmentDecrement  = " + getPredictedSegmentDecrement());
    }
    
    /**
     * High verbose output useful for debugging
     */
    public String getPrintString() {
        StringWriter sw;
        PrintWriter pw = new PrintWriter(sw = new StringWriter());
        
        pw.println("---------------------- General -------------------------");
        pw.println("columnDimensions           = " + Arrays.toString(getColumnDimensions()));
        pw.println("inputDimensions            = " + Arrays.toString(getInputDimensions()));
        pw.println("cellsPerColumn             = " + getCellsPerColumn());
        
        pw.println("random                     = " + getRandom());
        pw.println("seed                       = " + getSeed());
        
        pw.println("\n------------ SpatialPooler Parameters ------------------");
        pw.println("numInputs                  = " + getNumInputs());
        pw.println("numColumns                 = " + getNumColumns());
        pw.println("numActiveColumnsPerInhArea = " + getNumActiveColumnsPerInhArea());
        pw.println("potentialPct               = " + getPotentialPct());
        pw.println("potentialRadius            = " + getPotentialRadius());
        pw.println("globalInhibition           = " + getGlobalInhibition());
        pw.println("localAreaDensity           = " + getLocalAreaDensity());
        pw.println("inhibitionRadius           = " + getInhibitionRadius());
        pw.println("stimulusThreshold          = " + getStimulusThreshold());
        pw.println("synPermActiveInc           = " + getSynPermActiveInc());
        pw.println("synPermInactiveDec         = " + getSynPermInactiveDec());
        pw.println("synPermConnected           = " + getSynPermConnected());
        pw.println("synPermBelowStimulusInc    = " + getSynPermBelowStimulusInc());
        pw.println("synPermTrimThreshold       = " + getSynPermTrimThreshold());
        pw.println("minPctOverlapDutyCycles    = " + getMinPctOverlapDutyCycles());
        pw.println("minPctActiveDutyCycles     = " + getMinPctActiveDutyCycles());
        pw.println("dutyCyclePeriod            = " + getDutyCyclePeriod());
        pw.println("wrapAround                 = " + isWrapAround());
        pw.println("maxBoost                   = " + getMaxBoost());
        pw.println("version                    = " + getVersion());

        pw.println("\n------------ TemporalMemory Parameters ------------------");
        pw.println("activationThreshold        = " + getActivationThreshold());
        pw.println("learningRadius             = " + getLearningRadius());
        pw.println("minThreshold               = " + getMinThreshold());
        pw.println("maxNewSynapseCount         = " + getMaxNewSynapseCount());
        pw.println("maxSynapsesPerSegment      = " + getMaxSynapsesPerSegment());
        pw.println("maxSegmentsPerCell         = " + getMaxSegmentsPerCell());
        pw.println("initialPermanence          = " + getInitialPermanence());
        pw.println("connectedPermanence        = " + getConnectedPermanence());
        pw.println("permanenceIncrement        = " + getPermanenceIncrement());
        pw.println("permanenceDecrement        = " + getPermanenceDecrement());
        pw.println("predictedSegmentDecrement  = " + getPredictedSegmentDecrement());
        
        return sw.toString();
    }
    
    /**
     * Returns a 2 Dimensional array of 1's and 0's indicating
     * which of the column's pool members are above the connected
     * threshold, and therefore considered "connected"
     * @return
     */
    public int[][] getConnecteds() {
        int[][] retVal = new int[getNumColumns()][];
        for(int i = 0;i < getNumColumns();i++) {
            Pool pool = getPotentialPools().get(i);
            int[] indexes = pool.getDenseConnected(this);
            retVal[i] = indexes;
        }
        
        return retVal;
    }
    
    /**
     * Returns a 2 Dimensional array of 1's and 0's indicating
     * which input bits belong to which column's pool.
     * @return
     */
    public int[][] getPotentials() {
        int[][] retVal = new int[getNumColumns()][];
        for(int i = 0;i < getNumColumns();i++) {
            Pool pool = getPotentialPools().get(i);
            int[] indexes = pool.getDensePotential(this);
            retVal[i] = indexes;
        }
        
        return retVal;
    }
    
    /**
     * Returns a 2 Dimensional array of the permanences for SP
     * proximal dendrite column pooled connections.
     * @return
     */
    public double[][] getPermanences() {
        double[][] retVal = new double[getNumColumns()][];
        for(int i = 0;i < getNumColumns();i++) {
            Pool pool = getPotentialPools().get(i);
            double[] perm = pool.getDensePermanences(this);
            retVal[i] = perm;
        }
        
        return retVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + activationThreshold;
        result = prime * result + ((activeCells == null) ? 0 : activeCells.hashCode());
        result = prime * result + Arrays.hashCode(activeDutyCycles);
        result = prime * result + Arrays.hashCode(boostFactors);
        result = prime * result + Arrays.hashCode(cells);
        result = prime * result + cellsPerColumn;
        result = prime * result + Arrays.hashCode(columnDimensions);
        result = prime * result + ((connectedCounts == null) ? 0 : connectedCounts.hashCode());
        long temp;
        temp = Double.doubleToLongBits(connectedPermanence);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + dutyCyclePeriod;
        result = prime * result + (globalInhibition ? 1231 : 1237);
        result = prime * result + inhibitionRadius;
        temp = Double.doubleToLongBits(initConnectedPct);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(initialPermanence);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + Arrays.hashCode(inputDimensions);
        result = prime * result + ((inputMatrix == null) ? 0 : inputMatrix.hashCode());
        result = prime * result + spIterationLearnNum;
        result = prime * result + spIterationNum;
        result = prime * result + (new Long(tmIteration)).intValue();
        result = prime * result + learningRadius;
        temp = Double.doubleToLongBits(localAreaDensity);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxBoost);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + maxNewSynapseCount;
        result = prime * result + ((memory == null) ? 0 : memory.hashCode());
        result = prime * result + Arrays.hashCode(minActiveDutyCycles);
        result = prime * result + Arrays.hashCode(minOverlapDutyCycles);
        temp = Double.doubleToLongBits(minPctActiveDutyCycles);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minPctOverlapDutyCycles);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + minThreshold;
        temp = Double.doubleToLongBits(numActiveColumnsPerInhArea);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + numColumns;
        result = prime * result + numInputs;
        temp = numSynapses;
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + Arrays.hashCode(overlapDutyCycles);
        temp = Double.doubleToLongBits(permanenceDecrement);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(permanenceIncrement);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(potentialPct);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((potentialPools == null) ? 0 : potentialPools.hashCode());
        result = prime * result + potentialRadius;
        temp = Double.doubleToLongBits(predictedSegmentDecrement);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((predictiveCells == null) ? 0 : predictiveCells.hashCode());
        result = prime * result + ((random == null) ? 0 : random.hashCode());
        result = prime * result + ((receptorSynapses == null) ? 0 : receptorSynapses.hashCode());
        result = prime * result + seed;
        result = prime * result + ((segments == null) ? 0 : segments.hashCode());
        temp = Double.doubleToLongBits(stimulusThreshold);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermActiveInc);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermBelowStimulusInc);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermConnected);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermInactiveDec);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermMax);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermMin);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(synPermTrimThreshold);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + proximalSynapseCounter;
        result = prime * result + ((proximalSynapses == null) ? 0 : proximalSynapses.hashCode());
        result = prime * result + ((distalSynapses == null) ? 0 : distalSynapses.hashCode());
        result = prime * result + Arrays.hashCode(tieBreaker);
        result = prime * result + updatePeriod;
        temp = Double.doubleToLongBits(version);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + ((winnerCells == null) ? 0 : winnerCells.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        Connections other = (Connections)obj;
        if(activationThreshold != other.activationThreshold)
            return false;
        if(activeCells == null) {
            if(other.activeCells != null)
                return false;
        } else if(!activeCells.equals(other.activeCells))
            return false;
        if(!Arrays.equals(activeDutyCycles, other.activeDutyCycles))
            return false;
        if(!Arrays.equals(boostFactors, other.boostFactors))
            return false;
        if(!Arrays.equals(cells, other.cells))
            return false;
        if(cellsPerColumn != other.cellsPerColumn)
            return false;
        if(!Arrays.equals(columnDimensions, other.columnDimensions))
            return false;
        if(connectedCounts == null) {
            if(other.connectedCounts != null)
                return false;
        } else if(!connectedCounts.equals(other.connectedCounts))
            return false;
        if(Double.doubleToLongBits(connectedPermanence) != Double.doubleToLongBits(other.connectedPermanence))
            return false;
        if(dutyCyclePeriod != other.dutyCyclePeriod)
            return false;
        if(globalInhibition != other.globalInhibition)
            return false;
        if(inhibitionRadius != other.inhibitionRadius)
            return false;
        if(Double.doubleToLongBits(initConnectedPct) != Double.doubleToLongBits(other.initConnectedPct))
            return false;
        if(Double.doubleToLongBits(initialPermanence) != Double.doubleToLongBits(other.initialPermanence))
            return false;
        if(!Arrays.equals(inputDimensions, other.inputDimensions))
            return false;
        if(inputMatrix == null) {
            if(other.inputMatrix != null)
                return false;
        } else if(!inputMatrix.equals(other.inputMatrix))
            return false;
        if(spIterationLearnNum != other.spIterationLearnNum)
            return false;
        if(spIterationNum != other.spIterationNum)
            return false;
        if(tmIteration != other.tmIteration)
            return false;
        if(learningRadius != other.learningRadius)
            return false;
        if(Double.doubleToLongBits(localAreaDensity) != Double.doubleToLongBits(other.localAreaDensity))
            return false;
        if(Double.doubleToLongBits(maxBoost) != Double.doubleToLongBits(other.maxBoost))
            return false;
        if(maxNewSynapseCount != other.maxNewSynapseCount)
            return false;
        if(memory == null) {
            if(other.memory != null)
                return false;
        } else if(!memory.equals(other.memory))
            return false;
        if(!Arrays.equals(minActiveDutyCycles, other.minActiveDutyCycles))
            return false;
        if(!Arrays.equals(minOverlapDutyCycles, other.minOverlapDutyCycles))
            return false;
        if(Double.doubleToLongBits(minPctActiveDutyCycles) != Double.doubleToLongBits(other.minPctActiveDutyCycles))
            return false;
        if(Double.doubleToLongBits(minPctOverlapDutyCycles) != Double.doubleToLongBits(other.minPctOverlapDutyCycles))
            return false;
        if(minThreshold != other.minThreshold)
            return false;
        if(Double.doubleToLongBits(numActiveColumnsPerInhArea) != Double.doubleToLongBits(other.numActiveColumnsPerInhArea))
            return false;
        if(numColumns != other.numColumns)
            return false;
        if(numInputs != other.numInputs)
            return false;
        if(numSynapses != other.numSynapses)
            return false;
        if(!Arrays.equals(overlapDutyCycles, other.overlapDutyCycles))
            return false;
        if(Double.doubleToLongBits(permanenceDecrement) != Double.doubleToLongBits(other.permanenceDecrement))
            return false;
        if(Double.doubleToLongBits(permanenceIncrement) != Double.doubleToLongBits(other.permanenceIncrement))
            return false;
        if(Double.doubleToLongBits(potentialPct) != Double.doubleToLongBits(other.potentialPct))
            return false;
        if(potentialPools == null) {
            if(other.potentialPools != null)
                return false;
        } else if(!potentialPools.equals(other.potentialPools))
            return false;
        if(potentialRadius != other.potentialRadius)
            return false;
        if(Double.doubleToLongBits(predictedSegmentDecrement) != Double.doubleToLongBits(other.predictedSegmentDecrement))
            return false;
        if(predictiveCells == null) {
            if(other.predictiveCells != null)
                return false;
        } else if(!getPredictiveCells().equals(other.getPredictiveCells()))
            return false;
        if(receptorSynapses == null) {
            if(other.receptorSynapses != null)
                return false;
        } else if(!receptorSynapses.toString().equals(other.receptorSynapses.toString()))
            return false;
        if(seed != other.seed)
            return false;
        if(segments == null) {
            if(other.segments != null)
                return false;
        } else if(!segments.equals(other.segments))
            return false;
        if(Double.doubleToLongBits(stimulusThreshold) != Double.doubleToLongBits(other.stimulusThreshold))
            return false;
        if(Double.doubleToLongBits(synPermActiveInc) != Double.doubleToLongBits(other.synPermActiveInc))
            return false;
        if(Double.doubleToLongBits(synPermBelowStimulusInc) != Double.doubleToLongBits(other.synPermBelowStimulusInc))
            return false;
        if(Double.doubleToLongBits(synPermConnected) != Double.doubleToLongBits(other.synPermConnected))
            return false;
        if(Double.doubleToLongBits(synPermInactiveDec) != Double.doubleToLongBits(other.synPermInactiveDec))
            return false;
        if(Double.doubleToLongBits(synPermMax) != Double.doubleToLongBits(other.synPermMax))
            return false;
        if(Double.doubleToLongBits(synPermMin) != Double.doubleToLongBits(other.synPermMin))
            return false;
        if(Double.doubleToLongBits(synPermTrimThreshold) != Double.doubleToLongBits(other.synPermTrimThreshold))
            return false;
        if(proximalSynapseCounter != other.proximalSynapseCounter)
            return false;
        if(proximalSynapses == null) {
            if(other.proximalSynapses != null)
                return false;
        } else if(!proximalSynapses.equals(other.proximalSynapses))
            return false;
        if(distalSynapses == null) {
            if(other.distalSynapses != null)
                return false;
        } else if(!distalSynapses.equals(other.distalSynapses))
            return false;
        if(!Arrays.equals(tieBreaker, other.tieBreaker))
            return false;
        if(updatePeriod != other.updatePeriod)
            return false;
        if(Double.doubleToLongBits(version) != Double.doubleToLongBits(other.version))
            return false;
        if(winnerCells == null) {
            if(other.winnerCells != null)
                return false;
        } else if(!winnerCells.equals(other.winnerCells))
            return false;
        return true;
    }
}
