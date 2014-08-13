package org.numenta.nupic.research;

import org.numenta.nupic.research.Parameters.KEY;

public class SpatialPooler {
	private int[] inputDimensions = new int[] { 32, 32 };
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
	
	
	public SpatialPooler() {
		this(null);
	}
	
	public SpatialPooler(Parameters params) {
		if(params != null) {
			Parameters.apply(this, params);
		}
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
	 * spVerbosity level: 0, 1, 2, or 3
	 * 
	 * @param spVerbosity
	 */
	public void setSpVerbosity(int spVerbosity) {
		this.spVerbosity = spVerbosity;
	}
}
