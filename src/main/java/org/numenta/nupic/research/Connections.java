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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.numenta.nupic.data.SparseObjectMatrix;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Synapse;

/**
 * Represents the definition of the interconnected structural state of the
 * {@link TemporalMemory} as well as the state of all support structures 
 * (i.e. Cells, Columns, Segments, Synapses etc.)
 */
public class Connections {
    protected Set<Cell> activeCells = new LinkedHashSet<Cell>();
    protected Set<Cell> winnerCells = new LinkedHashSet<Cell>();
    protected Set<Cell> predictiveCells = new LinkedHashSet<Cell>();
    protected Set<Column> predictedColumns = new LinkedHashSet<Column>();
    protected Set<DistalDendrite> activeSegments = new LinkedHashSet<DistalDendrite>();
    protected Set<DistalDendrite> learningSegments = new LinkedHashSet<DistalDendrite>();
    protected Map<DistalDendrite, Set<Synapse>> activeSynapsesForSegment = new LinkedHashMap<DistalDendrite, Set<Synapse>>();
    
    /** Total number of columns */
    protected int[] columnDimensions = new int[] { 2048 };
    /** Total number of cells per column */
    protected int cellsPerColumn = 32;
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
    
    private SparseObjectMatrix<Column> memory;
    
    private Cell[] cells;

    
    ///////////////////////   Structural element state /////////////////////
    /** Reverse mapping from source cell to {@link Synapse} */
    protected Map<Cell, Set<Synapse>> receptorSynapses;
    
    protected Map<Cell, List<DistalDendrite>> segments;
    protected Map<DistalDendrite, List<Synapse>> synapses;
    
    /** Helps index each new Segment */
    protected int segmentCounter = 0;
    /** Helps index each new Synapse */
    protected int synapseCounter = 0;
    /** The default random number seed */
    protected int seed = 42;
    /** The random number generator */
    protected Random random = new Random(seed);
    
    
    
    /**
     * Constructs a new {@code Connections} object. Use
     * 
     */
    public Connections() {}
    
    /**
     * Clears all state.
     */
    public void clear() {
        activeCells.clear();
        winnerCells.clear();
        predictiveCells.clear();
        predictedColumns.clear();
        activeSegments.clear();
        learningSegments.clear();
        activeSynapsesForSegment.clear();
    }
    
    /**
     * Returns the {@link Cell} specified by the index passed in.
     * @param index		of the specified cell to return.
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
     * @param cellIndexes	indexes of the Cells to return
     * @return
     */
    public Cell[] getCells(int[] cellIndexes) {
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
     * @param cellIndexes	indexes of the Cells to return
     * @return
     */
    public LinkedHashSet<Cell> getCellSet(int[] cellIndexes) {
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
     * Sets the seed used for the internal random number generator.
     * If the generator has been instantiated, this method will initialize
     * a new random generator with the specified seed.
     * 
     * @param seed
     */
    public void setSeed(int seed) {
        random = new Random(seed);
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
     * Returns the current {@link Set} of active cells
     * 
     * @return  the current {@link Set} of active cells
     */
    public Set<Cell> getActiveCells() {
        return activeCells;
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
     * Returns the {@link Set} of predictive cells.
     * @return
     */
    public Set<Cell> getPredictiveCells() {
        return predictiveCells;
    }
    
    /**
     * Returns the current {@link Set} of predicted columns
     * 
     * @return  the current {@link Set} of predicted columns
     */
    public Set<Column> getPredictedColumns() {
        return predictedColumns;
    }
    
    /**
     * Returns the Set of learning {@link DistalDendrite}s
     * @return
     */
    public Set<DistalDendrite> getLearningSegments() {
        return learningSegments;
    }
    
    /**
     * Returns the Set of active {@link DistalDendrite}s
     * @return
     */
    public Set<DistalDendrite> getActiveSegments() {
        return activeSegments;
    }
    
    /**
     * Returns the mapping of Segments to active synapses in t-1
     * @return
     */
    public Map<DistalDendrite, Set<Synapse>> getActiveSynapsesForSegment() {
        return activeSynapsesForSegment;
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
        if(cell == null) {
            throw new IllegalArgumentException("Cell was null");
        }
        
        if(receptorSynapses == null) {
            receptorSynapses = new LinkedHashMap<Cell, Set<Synapse>>();
        }
        
        Set<Synapse> retVal = null;
        if((retVal = receptorSynapses.get(cell)) == null) {
            receptorSynapses.put(cell, retVal = new LinkedHashSet<Synapse>());
        }
        
        return retVal;
    }
    
    /**
     * Returns the mapping of {@link Cell}s to their {@link DistalDendrite}s.
     * 
     * @param cell      the {@link Cell} used as a key.
     * @return          the mapping of {@link Cell}s to their {@link DistalDendrite}s.
     */
    public List<DistalDendrite> getSegments(Cell cell) {
        if(cell == null) {
            throw new IllegalArgumentException("Cell was null");
        }
        
        if(segments == null) {
            segments = new LinkedHashMap<Cell, List<DistalDendrite>>();
        }
        
        List<DistalDendrite> retVal = null;
        if((retVal = segments.get(cell)) == null) {
            segments.put(cell, retVal = new ArrayList<DistalDendrite>());
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
        
        if(synapses == null) {
            synapses = new LinkedHashMap<DistalDendrite, List<Synapse>>();
        }
        
        List<Synapse> retVal = null;
        if((retVal = synapses.get(segment)) == null) {
            synapses.put(segment, retVal = new ArrayList<Synapse>());
        }
        
        return retVal;
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
     * Sets the number of {@link Cell}s per {@link Column}
     * @param cellsPerColumn
     */
    public void setCellsPerColumn(int cellsPerColumn) {
        this.cellsPerColumn = cellsPerColumn;
    }
    
    /**
     * Gets the number of {@link Cells} per {@link Column}.
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
     * cell in a bursing column.
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
     * learing.
     * 
     * @return
     */
    public int getMaxNewSynapseCount() {
    	return maxNewSynapseCount;
    }

    /** 
     * Initial permanence of a new synapse 
     * 
     * @param   
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
     * 
     * @param   permanenceIncrement
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
     * 
     * @param   permanenceDecrement
     */
    public double getPermanenceDecrement() {
        return this.permanenceDecrement;
    }
    
    /**
     * Converts a {@link Collection} of {@link Cell}s to a list
     * of cell indexes.
     * 
     * @param cells
     * @return
     */
    public List<Integer> asCellIndexes(Collection<Cell> cells) {
        List<Integer> ints = new ArrayList<Integer>();
        for(Cell cell : cells) {
            ints.add(cell.getIndex());
        }
        
        return ints;
    }
    
    /**
     * Converts a {@link Collection} of {@link Columns}s to a list
     * of column indexes.
     * 
     * @param columns
     * @return
     */
    public List<Integer> asColumnIndexes(Collection<Column> columns) {
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
}
