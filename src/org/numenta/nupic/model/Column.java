package org.numenta.nupic.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.numenta.nupic.research.TemporalMemory;

/**
 * Abstraction of both an input bit and a columnal collection of
 * {@link Cell}s which have behavior associated with membership to
 * a given {@code Column}
 * 
 * @author David Ray
 *
 */
public class Column {
	private final int index;
	
	private final int numCells;
	
	private Cell[] cells;
	
	/**
	 * Constructs a new {@code Column}
	 * 
	 * @param numCells		number of cells per column
	 * @param index			the index of this column
	 */
	public Column(int numCells, int index) {
		this.numCells = numCells;
		this.index = index;
		cells = new Cell[numCells];
		for(int i = 0;i < numCells;i++) {
			cells[i] = new Cell(this, i);
		}
	}
	
	/**
	 * Returns the {@link Cell} residing at the specified index.
	 * 
	 * @param index		the index of the {@link Cell} to return.
	 * @return			the {@link Cell} residing at the specified index.
	 */
	public Cell getCell(int index) {
		return cells[index];
	}
	
	/**
	 * Returns a {@link List} view of this {@code Column}'s {@link Cell}s.
	 * @return
	 */
	public List<Cell> getCells() {
		return Arrays.asList(cells);
	}
	
	/**
	 * Returns the index of this {@code Column}
	 * @return	the index of this {@code Column}
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * Returns the configured number of cells per column for
	 * all {@code Column} objects within the current {@link TemporalMemory}
	 * @return
	 */
	public int getNumCellsPerColumn() {
		return numCells;
	}
	
	/**
	 * Returns the {@link Cell} with the least number of {@link Segment}s.
	 * @param random
	 * @return
	 */
	public Cell getLeastUsedCell(Random random) {
		List<Cell> cells = getCells();
		List<Cell> leastUsedCells = new ArrayList<Cell>();
		int minNumSegments = Integer.MAX_VALUE;
		
		for(Cell cell : cells) {
			int numSegments = cell.getSegments().size();
			
			if(numSegments < minNumSegments) {
				minNumSegments = numSegments;
				leastUsedCells = new ArrayList<Cell>();
			}
			
			if(numSegments == minNumSegments) {
				leastUsedCells.add(cell);
			}
		}
		int index = random.nextInt(leastUsedCells.size());
		Collections.sort(leastUsedCells);
		return leastUsedCells.get(index); 
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return "Column: idx=" + index;
	}
}
