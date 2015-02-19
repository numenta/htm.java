package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import org.junit.Test;
import org.numenta.nupic.algorithms.MovingAverage.Calculation;

/**
 * Test that the (internal) moving average maintains the averages correctly,
 * even for null initial condition and when the number of values goes over
 * windowSize.  Pass in integers and floats.
 * 
 * @author Numenta
 * @author David Ray
 */
public class MovingAverageTest {

    
    @Test
    public void testMovingAverage() {
        TDoubleList historicalValues = new TDoubleArrayList();
        double total = 0;
        int windowSize = 3;
        TDoubleList expected = new TDoubleArrayList(new double[]{ 3.0 });
        Calculation result = MovingAverage.compute(historicalValues, total, 3, windowSize);
        assertEquals(3.0, result.getAverage(), 1.0);
        assertEquals(expected, result.getHistoricalValues());
        assertEquals(3.0, result.getTotal(), 1.0);
        
        expected = new TDoubleArrayList(new double[]{ 3.0, 4.0 });
        result = MovingAverage.compute(historicalValues, result.getTotal(), 4, windowSize);
        assertEquals(3.5, result.getAverage(), 1.0);
        assertEquals(expected, result.getHistoricalValues());
        assertEquals(7.0, result.getTotal(), 1.0);
        
        expected = new TDoubleArrayList(new double[]{ 3.0, 4.0, 5.0 });
        result = MovingAverage.compute(historicalValues, result.getTotal(), 5.0, windowSize);
        assertEquals(4.0, result.getAverage(), 1.0);
        assertEquals(expected, result.getHistoricalValues());
        assertEquals(12.0, result.getTotal(), 1.0);
        
        // Ensure the first value gets removed
        expected = new TDoubleArrayList(new double[]{ 4.0, 5.0, 6.0 });
        result = MovingAverage.compute(historicalValues, result.getTotal(), 6.0, windowSize);
        assertEquals(5.0, result.getAverage(), 1.0);
        assertEquals(expected, result.getHistoricalValues());
        assertEquals(15.0, result.getTotal(), 1.0);
    }
    
    @Test
    public void testMovingAverageInstance() {
        MovingAverage ma = new MovingAverage(null, 3);
        
        TDoubleList expected = new TDoubleArrayList(new double[]{ 3.0 });
        double newAverage = ma.next(3);
        assertEquals(3.0, newAverage, 1.0);
        assertEquals(expected, ma.getSlidingWindow());
        assertEquals(3.0, ma.getTotal(), 1.0);
        
        expected = new TDoubleArrayList(new double[]{ 3.0, 4.0 });
        newAverage = ma.next(4.0);
        assertEquals(3.5, newAverage, 1.0);
        assertEquals(expected, ma.getSlidingWindow());
        assertEquals(7.0, ma.getTotal(), 1.0);
        
        expected = new TDoubleArrayList(new double[]{ 3.0, 4.0, 5.0 });
        newAverage = ma.next(5);
        assertEquals(4.0, newAverage, 1.0);
        assertEquals(expected, ma.getSlidingWindow());
        assertEquals(12.0, ma.getTotal(), 1.0);
        
        // Ensure the first value gets removed
        expected = new TDoubleArrayList(new double[]{ 4.0, 5.0, 6.0 });
        newAverage = ma.next(6);
        assertEquals(5.0, newAverage, 1.0);
        assertEquals(expected, ma.getSlidingWindow());
        assertEquals(15.0, ma.getTotal(), 1.0);
    }
    
    /**
     * Test the slidingWindow value is correctly assigned when initializing a
     * new MovingAverage object.
     */
    @Test
    public void testMovingAverageSlidingWindowInit() {
        MovingAverage ma = new MovingAverage(new TDoubleArrayList(new double[] { 3., 4., 5., }), 3);
        assertEquals(new TDoubleArrayList(new double[] { 3., 4., 5., }), ma.getSlidingWindow());
        
        ma = new MovingAverage(null, 3);
        assertEquals(new TDoubleArrayList(), ma.getSlidingWindow());
    }

}
