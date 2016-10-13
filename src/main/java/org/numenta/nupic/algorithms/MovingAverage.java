/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.algorithms;

import org.numenta.nupic.model.Persistable;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

/**
 * Helper class for computing moving average and sliding window
 * 
 * @author Numenta
 * @author David Ray
 */
public class MovingAverage implements Persistable {
    private static final long serialVersionUID = 1L;

    private Calculation calc;
    
    private int windowSize;
    
    /**
     * Constructs a new {@code MovingAverage}
     * 
     * @param historicalValues  list of entry values
     * @param windowSize        length over which to take the average
     */
    public MovingAverage(TDoubleList historicalValues, int windowSize) {
        this(historicalValues, -1, windowSize);
    }
    
    /**
     * Constructs a new {@code MovingAverage}
     * 
     * @param historicalValues  list of entry values
     * @param windowSize        length over which to take the average
     */
    public MovingAverage(TDoubleList historicalValues, double total, int windowSize) {
        if(windowSize <= 0) {
            throw new IllegalArgumentException("Window size must be > 0");
        }
        this.windowSize = windowSize;
        
        calc = new Calculation();
        calc.historicalValues = 
            historicalValues == null || historicalValues.size() < 1 ?
                new TDoubleArrayList(windowSize) : historicalValues;
        calc.total = total != -1 ? total : calc.historicalValues.sum();
    }
    
    /**
     * Routine for computing a moving average
     * 
     * @param slidingWindow     a list of previous values to use in the computation that
     *                          will be modified and returned
     * @param total             total the sum of the values in the  slidingWindow to be used in the
     *                          calculation of the moving average
     * @param newVal            newVal a new number to compute the new windowed average
     * @param windowSize        windowSize how many values to use in the moving window
     * @return
     */
    public static Calculation compute(TDoubleList slidingWindow, double total, double newVal, int windowSize) {
        return compute(null, slidingWindow, total, newVal, windowSize);
    }
    
    /**
     * Internal method which does actual calculation
     * 
     * @param calc              Re-used calculation object
     * @param slidingWindow     a list of previous values to use in the computation that
     *                          will be modified and returned
     * @param total             total the sum of the values in the  slidingWindow to be used in the
     *                          calculation of the moving average
     * @param newVal            newVal a new number to compute the new windowed average
     * @param windowSize        windowSize how many values to use in the moving window
     * @return
     */
    private static Calculation compute(
        Calculation calc, TDoubleList slidingWindow, double total, double newVal, int windowSize) {
        
        if(slidingWindow == null) {
            throw new IllegalArgumentException("slidingWindow cannot be null.");
        }
        
        if(slidingWindow.size() == windowSize) {
            total -= slidingWindow.removeAt(0);
        }
        slidingWindow.add(newVal);
        total += newVal;
        
        if(calc == null) {
            return new Calculation(slidingWindow, total / (double)slidingWindow.size(), total);
        }
        
        return copyInto(calc, slidingWindow, total / (double)slidingWindow.size(), total);
    }
    
    /**
     * Called to compute the next moving average value.
     * 
     * @param newValue  new point data
     * @return
     */
    public double next(double newValue) {
        compute(calc, calc.historicalValues, calc.total, newValue, windowSize);
        return calc.average;
    }
    
    /**
     * Returns the sliding window buffer used to calculate the moving average.
     * @return
     */
    public TDoubleList getSlidingWindow() {
        return calc.historicalValues;
    }
    
    /**
     * Returns the current running total
     * @return
     */
    public double getTotal() {
        return calc.total;
    }
    
    /**
     * Returns the size of the window over which the 
     * moving average is computed.
     * 
     * @return
     */
    public int getWindowSize() {
        return windowSize;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((calc == null) ? 0 : calc.hashCode());
        result = prime * result + windowSize;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        MovingAverage other = (MovingAverage)obj;
        if(calc == null) {
            if(other.calc != null)
                return false;
        } else if(!calc.equals(other.calc))
            return false;
        if(windowSize != other.windowSize)
            return false;
        return true;
    }

    /**
     * Internal method to update running totals.
     * 
     * @param c
     * @param slidingWindow
     * @param value
     * @param total
     * @return
     */
    private static Calculation copyInto(Calculation c, TDoubleList slidingWindow, double average, double total) {
        c.historicalValues = slidingWindow;
        c.average = average;
        c.total = total;
        return c;
    }
    
    /**
     * Container for calculated data
     */
    public static class Calculation implements Persistable {
        private static final long serialVersionUID = 1L;
        
        private  double average;
        private TDoubleList historicalValues;
        private double total;
        
        public Calculation() {
            
        }
        
        public Calculation(TDoubleList historicalValues, double currentValue, double total) {
            this.average = currentValue;
            this.historicalValues = historicalValues;
            this.total = total;
        }
        
        /**
         * Returns the current value at this point in the calculation.
         * @return
         */
        public double getAverage() {
            return average;
        }
        
        /**
         * Returns a list of calculated values in the order of their
         * calculation.
         * 
         * @return
         */
        public TDoubleList getHistoricalValues() {
            return historicalValues;
        }
        
        /**
         * Returns the total
         * @return
         */
        public double getTotal() {
            return total;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(average);
            result = prime * result + (int)(temp ^ (temp >>> 32));
            result = prime * result + ((historicalValues == null) ? 0 : historicalValues.hashCode());
            temp = Double.doubleToLongBits(total);
            result = prime * result + (int)(temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Calculation other = (Calculation)obj;
            if(Double.doubleToLongBits(average) != Double.doubleToLongBits(other.average))
                return false;
            if(historicalValues == null) {
                if(other.historicalValues != null)
                    return false;
            } else if(!historicalValues.equals(other.historicalValues))
                return false;
            if(Double.doubleToLongBits(total) != Double.doubleToLongBits(other.total))
                return false;
            return true;
        }
        
    }
}
