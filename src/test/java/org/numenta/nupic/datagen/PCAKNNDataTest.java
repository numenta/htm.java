package org.numenta.nupic.datagen;

import static org.junit.Assert.*;

import org.junit.Test;


public class PCAKNNDataTest {

    @Test
    public void testGenerateForPCAKNNShort() {
        PCAKNNData data = new PCAKNNData();
        KNNDataArray[] dataArray = data.getPCAKNNShortData();
        assertNotNull(dataArray);
        assertEquals(2, dataArray.length);
        for(int i = 0;i < 2;i++) {
            switch(i) {
                case 0: { // Training Data
                    assertNotNull(dataArray[i].getClassArray());
                    assertEquals(900, dataArray[i].getClassArray().length);
                    assertNotNull(dataArray[i].getDataArray());
                    assertEquals(900, dataArray[i].getDataArray().length);
                    break;
                }
                case 1: { // Actual Data
                    assertNotNull(dataArray[i].getClassArray());
                    assertEquals(100, dataArray[i].getClassArray().length);
                    assertNotNull(dataArray[i].getDataArray());
                    assertEquals(100, dataArray[i].getDataArray().length);
                    break;
                }
            }
        }
    }

}
