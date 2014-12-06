/**
 * Copyright (c) 2011, Peace Technology, Inc.
 * $Author:$
 * $Revision:$
 * $Date:$
 * $NoKeywords$
 */

package org.numenta.nupic.encoders;

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SDRCategoryEncoderTest {

    @Test
    public void testSDRCategoryEncoder() {
        System.out.println("Testing CategoryEncoder...");
        //make sure we have>16 categories so that we have to grow our sdrs
        String[] categories ={"ES", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8",
                "S9", "S10", "S11", "S12", "S13", "S14", "S15", "S16",
                "S17", "S18", "S19", "GB", "US"};

        int fieldWidth = 100;
        int bitsOn = 10;

        SDRCategoryEncoder sdrCategoryEncoder = SDRCategoryEncoder.builder()
                .setN(fieldWidth)
                .setW(bitsOn)
                .setCategoryList(new LinkedHashSet<>(Arrays.asList(categories)))
                .setName("foo")
                .setVerbosity(2)
                .setForced(true).build();

       //internal check
        assertEquals(sdrCategoryEncoder.getSdrs().size(), 23);
        assertEquals(sdrCategoryEncoder.getSdrs().get(0).length, fieldWidth);
        //ES
        int[] es = sdrCategoryEncoder.encode("ES");
        assertEquals(ArrayUtils.aggregateArray(es), bitsOn);
        assertEquals(es.length, fieldWidth);

        DecodeResult x = sdrCategoryEncoder.decode(es);
        //assertIsInstance(x[0], dict) - NO NEEDED IN JAVA
        assertTrue("foo".equals(x.getDescriptions().iterator().next()));
        assertTrue("ES".equals(x.getFields().get("foo").getDescription()));
    }

}
