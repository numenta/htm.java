package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClassificationTest {
    @Test
    public void testCopy() {
        String mon = "Monday";
        String tue = "Tuesday";
        String wed = "Wednesday";
        
        double monVal = 0.01d;
        double tueVal = 0.80d;
        double wedVal = 0.30d;
        
        Classification<String> result = new Classification<>();
        result.setActualValues(new String[] { mon, tue, wed });
        result.setStats(1, new double[] { monVal, tueVal, wedVal });
        assertTrue(result.getMostProbableValue(1).equals(tue));
        assertNull(result.getMostProbableValue(2));
        
        Classification<String> result2 = result.copy();
        assertEquals(result, result2);
        
        result2.setStats(1, new double[] { monVal, tueVal, 0.5d });
        assertNotEquals(result, result2);
    }

	@Test
	public void testGetMostProbableValue() {
		String mon = "Monday";
		String tue = "Tuesday";
		String wed = "Wednesday";
		
		double monVal = 0.01d;
		double tueVal = 0.80d;
		double wedVal = 0.30d;
		
		Classification<String> result = new Classification<>();
		result.setActualValues(new String[] { mon, tue, wed });
		result.setStats(1, new double[] { monVal, tueVal, wedVal });
		assertTrue(result.getMostProbableValue(1).equals(tue));
		assertNull(result.getMostProbableValue(2));
		
		double monVal2 = 0.30d;
		double tueVal2 = 0.01d;
		double wedVal2 = 0.29d;
		result.setStats(3, new double[] { monVal2, tueVal2, wedVal2 });
		assertTrue(result.getMostProbableValue(3).equals(mon));
		assertNull(result.getMostProbableValue(2));
	}
	
	@Test
	public void testGetMostProbableBucketIndex() {
		String mon = "Monday";
		String tue = "Tuesday";
		String wed = "Wednesday";
		
		double monVal = 0.01d;
		double tueVal = 0.80d;
		double wedVal = 0.30d;
		
		Classification<String> result = new Classification<>();
		result.setActualValues(new String[] { mon, tue, wed });
		result.setStats(1, new double[] { monVal, tueVal, wedVal });
		assertTrue(result.getMostProbableBucketIndex(1) == 1);
		assertTrue(result.getMostProbableBucketIndex(2) == -1);
		
		double monVal2 = 0.30d;
		double tueVal2 = 0.01d;
		double wedVal2 = 0.29d;
		result.setStats(3, new double[] { monVal2, tueVal2, wedVal2 });
		assertTrue(result.getMostProbableBucketIndex(3) == 0);
		assertTrue(result.getMostProbableBucketIndex(2) == -1);
	}

    @Test
    public void testGetCorrectStepsCount() {
        String mon = "Monday";
        String tue = "Tuesday";
        String wed = "Wednesday";

        double monVal = 0.01d;
        double tueVal = 0.80d;
        double wedVal = 0.30d;

        Classification<String> result = new Classification<>();
        result.setActualValues(new String[] { mon, tue, wed });
        result.setStats(1, new double[] { monVal, tueVal, wedVal });
        assertTrue(result.getMostProbableBucketIndex(1) == 1);
        assertTrue(result.getMostProbableBucketIndex(2) == -1);

        double monVal2 = 0.30d;
        double tueVal2 = 0.01d;
        double wedVal2 = 0.29d;
        result.setStats(3, new double[] { monVal2, tueVal2, wedVal2 });
        assertTrue(result.getMostProbableBucketIndex(3) == 0);
        assertTrue(result.getMostProbableBucketIndex(2) == -1);
        assertTrue(result.getStepCount() == 2);
    }
}
