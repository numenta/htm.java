package org.numenta.nupic.research;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Random;

import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;

/**
 * Specifies parameters to be used as a configuration for a given {@link TemporalMemory}
 * 
 * @author David Ray
 * 
 * @see TemporalMemory
 * @see ComputeCycle
 */
@SuppressWarnings("unused")
public class Parameters {
	/**
	 * Constant values representing configuration parameters for the {@link TemporalMemory}
	 */
	public enum KEY { 
		COLUMN_DIMENSIONS("columnDimensions", new int[] { 2048 }, true),
		CELLS_PER_COLUMN("cellsPerColumn", 32, false),
		ACTIVATION_THRESHOLD("activationThreshold", 13, false),
		LEARNING_RADIUS("learningRadius", 2048, false),
		MIN_THRESHOLD("minThreshold", 10, false),
		MAX_NEW_SYNAPSE_COUNT("maxNewSynapseCount", 20, false),
		INITIAL_PERMANENCE("initialPermanence", 0.21, false),
		CONNECTED_PERMANENCE("connectedPermanence", 0.5, false),
		PERMANENCE_INCREMENT("permanenceIncrement", 0.10, false), 
		PERMANENCE_DECREMENT("permanenceDecrement", 0.10, false),
		RANDOM("random", -1, false),
		SEED("seed", 42, false);
		
		private String fieldName;
		private Object fieldValue;
		private boolean isDimensional;
		
		private KEY(String fieldName, Object fieldValue, boolean isDimensional) {
			this.fieldName = fieldName;
			this.fieldValue = fieldValue;
			this.isDimensional = isDimensional;
		}
	};
	
	/** Total number of columns */
	private int[] columnDimensions = new int[] { 2048 };
	/** Total number of cells per column */
	private int cellsPerColumn = 32;
	/** 
	 * If the number of active connected synapses on a segment 
	 * is at least this threshold, the segment is said to be active.
     */
	private int activationThreshold = 13;
	/**
	 * Radius around cell from which it can
     * sample to form distal {@link Dendrite} connections.
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
	/** Seed for random number generator */
	private int seed = 42;
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
	/** Random Number Generator */
	private Random random;
	/** Map of parameters to their values */
	private EnumMap<Parameters.KEY, Object> paramMap;
	
	
	public Parameters() {}
	
	public Parameters(int[] columnDimensions, int cellsPerColumn,
			int activationThreshold, int learningRadius, int minThreshold,
			int maxNewSynapseCount, int seed, double initialPermanence,
			double connectedPermanence, double permanenceIncrement,
			double permanenceDecrement, Random random) {
		super();
		setColumnDimensions(columnDimensions);
		setCellsPerColumn(cellsPerColumn);
		setActivationThreshold(activationThreshold);
		setLearningRadius(learningRadius);
		setMinThreshold(minThreshold);
		setMaxNewSynapseCount(maxNewSynapseCount);
		setSeed(seed);
		setInitialPermanence(initialPermanence);
		setConnectedPermanence(connectedPermanence);
		setPermanenceIncrement(permanenceIncrement);
		setPermanenceDecrement(permanenceDecrement);
		setRandom(random);
	}
	
	/**
	 * Creates and returns an {@link EnumMap} containing the specified keys; whose
	 * values are to be loaded later. The map returned is only created if the internal
	 * reference is null (never been created), therefore the map is a singleton which
	 * cannot be recreated once created.
	 * 
	 * @return
	 */
	public EnumMap<Parameters.KEY, Object> getMap() {
		if(paramMap == null) {
			paramMap = new EnumMap<Parameters.KEY, Object>(Parameters.KEY.class);
		}
		
		return paramMap;
	}
	
	/**
	 * Sets the fields specified by the {@code Paremeters} on the specified
	 * {@link TemporalMemory}
	 * 
	 * @param tm
	 * @param p
	 */
	public static void apply(TemporalMemory tm, Parameters p) {
		try {
			for(Parameters.KEY key : p.paramMap.keySet()) {
				switch(key){
					case RANDOM: {
						Field f = tm.getClass().getDeclaredField(key.fieldName);
						f.setAccessible(true);
						f.set(tm, p.random);
						
						f = p.getClass().getDeclaredField(key.fieldName);
						f.setAccessible(true);
						f.set(p, p.random);
						
						break;
					}
					default: {
						Field f = tm.getClass().getDeclaredField(key.fieldName);
						f.setAccessible(true);
						f.set(tm, p.paramMap.get(key));
						
						f = p.getClass().getDeclaredField(key.fieldName);
						f.setAccessible(true);
						f.set(p, p.paramMap.get(key));
						
						break;
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the seeded random number generator.
	 * @param	r	the generator to use.
	 */
	public void setRandom(Random r) {
		this.random = r;
	}

	/**
	 * Sets the number of {@link Column}.
	 * 
	 * @param columnDimensions
	 */
	public void setColumnDimensions(int[] columnDimensions) {
		this.columnDimensions = columnDimensions;
		getMap().put(KEY.COLUMN_DIMENSIONS, columnDimensions);
	}

	/**
	 * Sets the number of {@link Cell}s per {@link Column}
	 * @param cellsPerColumn
	 */
	public void setCellsPerColumn(int cellsPerColumn) {
		this.cellsPerColumn = cellsPerColumn;
		getMap().put(KEY.CELLS_PER_COLUMN, cellsPerColumn);
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
		getMap().put(KEY.ACTIVATION_THRESHOLD, activationThreshold);
	}

	/**
	 * Radius around cell from which it can
     * sample to form distal dendrite connections.
     * 
     * @param	learningRadius
	 */
	public void setLearningRadius(int learningRadius) {
		this.learningRadius = learningRadius;
		getMap().put(KEY.LEARNING_RADIUS, learningRadius);
	}

	/**
	 * If the number of synapses active on a segment is at least this
	 * threshold, it is selected as the best matching
     * cell in a bursing column.
     * 
     * @param	minThreshold
	 */
	public void setMinThreshold(int minThreshold) {
		this.minThreshold = minThreshold;
		getMap().put(KEY.MIN_THRESHOLD, minThreshold);
	}

	/** 
	 * The maximum number of synapses added to a segment during learning. 
	 * 
	 * @param	maxNewSynapseCount
	 */
	public void setMaxNewSynapseCount(int maxNewSynapseCount) {
		this.maxNewSynapseCount = maxNewSynapseCount;
		getMap().put(KEY.MAX_NEW_SYNAPSE_COUNT, maxNewSynapseCount);
	}

	/** 
	 * Seed for random number generator 
	 * 
	 * @param	seed
	 */
	public void setSeed(int seed) {
		this.seed = seed;
		getMap().put(KEY.SEED, seed);
	}
	
	/** 
	 * Initial permanence of a new synapse 
	 * 
	 * @param	
	 */
	public void setInitialPermanence(double initialPermanence) {
		this.initialPermanence = initialPermanence;
		getMap().put(KEY.INITIAL_PERMANENCE, initialPermanence);
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
		getMap().put(KEY.CONNECTED_PERMANENCE, connectedPermanence);
	}

	/** 
	 * Amount by which permanences of synapses
	 * are incremented during learning.
	 * 
	 * @param 	permanenceIncrement
	 */
	public void setPermanenceIncrement(double permanenceIncrement) {
		this.permanenceIncrement = permanenceIncrement;
		getMap().put(KEY.PERMANENCE_INCREMENT, permanenceIncrement);
	}
	
	/** 
	 * Amount by which permanences of synapses
	 * are decremented during learning.
	 * 
	 * @param	permanenceDecrement
	 */
	public void setPermanenceDecrement(double permanenceDecrement) {
		this.permanenceDecrement = permanenceDecrement;
		getMap().put(KEY.PERMANENCE_DECREMENT, permanenceDecrement);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n")
		.append("\t").append("activationThreshold :  ").append(activationThreshold).append("\n")
		.append("\t").append("cellsPerColumn :  ").append(cellsPerColumn).append("\n")
		.append("\t").append("columnDimensions :  ").append(columnDimensions).append("\n")
		.append("\t").append("connectedPermanence :  ").append(connectedPermanence).append("\n")
		.append("\t").append("initialPermanence :  ").append(initialPermanence).append("\n")
		.append("\t").append("maxNewSynapseCount :  ").append(maxNewSynapseCount).append("\n")
		.append("\t").append("minThreshold :  ").append(minThreshold).append("\n")
		.append("\t").append("permanenceIncrement :  ").append(permanenceIncrement).append("\n")
		.append("\t").append("permanenceDecrement :  ").append(permanenceDecrement).append("\n")
		.append("}\n\n");
		
		return sb.toString();
	}
	
}
