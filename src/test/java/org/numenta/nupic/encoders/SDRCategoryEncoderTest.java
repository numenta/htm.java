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

package org.numenta.nupic.encoders;

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SDRCategoryEncoderTest {

    @Test
    public void testSDRCategoryEncoder() {
        System.out.println("Testing CategoryEncoder...");
        //make sure we have>16 categories so that we have to grow our sdrs
        String[] categories = {"ES", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8",
                               "S9", "S10", "S11", "S12", "S13", "S14", "S15", "S16",
                               "S17", "S18", "S19", "GB", "US"};

        int fieldWidth = 100;
        int bitsOn = 10;

        SDRCategoryEncoder sdrCategoryEncoder = SDRCategoryEncoder.builder()
                .n(fieldWidth)
                .w(bitsOn)
                .categoryList(Arrays.asList(categories))
                .name("foo")
                .forced(true).build();

        //internal check
        assertEquals(sdrCategoryEncoder.getSDRs().size(), 23);
        assertEquals(sdrCategoryEncoder.getSDRs().iterator().next().length, fieldWidth);
        //ES
        int[] es = sdrCategoryEncoder.encode("ES");
        assertEquals(ArrayUtils.aggregateArray(es), bitsOn);
        assertEquals(es.length, fieldWidth);

        DecodeResult x = sdrCategoryEncoder.decode(es);
        //assertIsInstance(x[0], dict) - NOT NEEDED IN JAVA
        assertTrue("foo".equals(x.getDescriptions().iterator().next()));
        assertTrue("ES".equals(x.getFields().get("foo").getDescription()));

        List<Encoding> topDowns = sdrCategoryEncoder.topDownCompute(es);
        Encoding topDown = topDowns.get(0);
        assertEquals(topDown.getValue(), "ES");
        assertEquals(topDown.getScalar(), 1);
        assertEquals(ArrayUtils.aggregateArray(topDown.getEncoding()), bitsOn);


        //Test topDown compute
        for (String category : categories) {
            int[] output = sdrCategoryEncoder.encode(category);
            topDown = sdrCategoryEncoder.topDownCompute(output).get(0);
            assertEquals(topDown.getValue(), category);
            assertEquals(topDown.getScalar(), (int)sdrCategoryEncoder.getScalars(category).get(0));
            int[] bucketIndices = sdrCategoryEncoder.getBucketIndices(category);
            System.out.print("bucket index =>" + bucketIndices[0]);
        }

        //Unknown
        int[] unknown = sdrCategoryEncoder.encode("ASDFLKJLK");
        assertEquals(ArrayUtils.aggregateArray(unknown), bitsOn);
        assertEquals(unknown.length, (fieldWidth));
        x = sdrCategoryEncoder.decode(unknown);
        assertEquals(x.getFields().get("foo").getDescription(), "<UNKNOWN>");

        topDown = sdrCategoryEncoder.topDownCompute(unknown).get(0);
        assertEquals(topDown.getValue(), "<UNKNOWN>");
        assertEquals(topDown.getScalar(), 0);

        //US
        int[] us = sdrCategoryEncoder.encode("US");
        assertEquals(ArrayUtils.aggregateArray(us), bitsOn);
        assertEquals(us.length, (fieldWidth));
        assertEquals(ArrayUtils.aggregateArray(us), bitsOn);
        x = sdrCategoryEncoder.decode(us);
        assertEquals(x.getFields().get("foo").getDescription(), "US");

        topDown = sdrCategoryEncoder.topDownCompute(us).get(0);
        assertEquals(topDown.getValue(), "US");
        assertEquals(topDown.getScalar(), categories.length);
        assertEquals(ArrayUtils.aggregateArray(topDown.getEncoding()), bitsOn);

        // empty field
        String[] emptyValues = {null, ""};
        for (String emptyValue : emptyValues) {
            int[] empty = sdrCategoryEncoder.encode(emptyValue);
            assertEquals(ArrayUtils.aggregateArray(empty), 0);
            assertEquals(empty.length, (fieldWidth));
        }

        //make sure it can still be decoded after a change
        int bit = new Random().nextInt(sdrCategoryEncoder.getWidth() - 1);
        us[bit] = 1 - us[bit];
        x = sdrCategoryEncoder.decode(us);
        assertEquals(x.getFields().get("foo").getDescription(), "US");

        //add two reps together
        int[] newrep = ArrayUtils.or(unknown, us);
        x = sdrCategoryEncoder.decode(newrep);
        String name = x.getFields().get("foo").getDescription();
        if ("US <UNKNOWN>".equals(name) && "<UNKNOWN> US".equals(name)) {
            String othercategory = name.replace("US", "");
            othercategory = othercategory.replace("<UNKNOWN>", "");
            othercategory = othercategory.replace(" ", "");
            System.out.println(String.format("Got: %s instead of US/<UNKNOWN>", name));
            System.out.println(String.format("US: %s", ArrayUtils.intArrayToString(us)));
            System.out.println(String.format("unknown: %s", ArrayUtils.intArrayToString(unknown)));
            System.out.println(String.format("Sum: %s", ArrayUtils.intArrayToString(newrep)));
            System.out.println(String.format("%s: %s", othercategory, ArrayUtils.intArrayToString(
                    sdrCategoryEncoder.encode(othercategory))));
            throw new RuntimeException("Decoding failure");
        }

        sdrCategoryEncoder = SDRCategoryEncoder.builder()
                .n(fieldWidth)
                .w(bitsOn)
                .name("bar")
                .forced(true).build();
        es = sdrCategoryEncoder.encode("ES");
        assertEquals(ArrayUtils.aggregateArray(es), bitsOn);
        assertEquals(es.length, (fieldWidth));
        x = sdrCategoryEncoder.decode(es);
        assertEquals(x.getDescriptions().get(0), "bar");
        assertEquals(x.getFields().get("bar").getDescription(), "ES");

        us = sdrCategoryEncoder.encode("US");
        assertEquals(ArrayUtils.aggregateArray(us), bitsOn);
        assertEquals(us.length, (fieldWidth));
        x = sdrCategoryEncoder.decode(us);
        assertEquals(x.getDescriptions().get(0), "bar");
        assertEquals(x.getFields().get("bar").getDescription(), "US");
        x = sdrCategoryEncoder.decode(us);
        assertEquals(x.getFields().get("bar").getDescription(), "US");

        int[] es2 = sdrCategoryEncoder.encode("ES");
        assertTrue(Arrays.equals(es, es2));

        int[] us2 = sdrCategoryEncoder.encode("US");
        assertTrue(Arrays.equals(us, us2));

        //make sure it can still be decoded after a change

        bit = new Random().nextInt(sdrCategoryEncoder.getWidth() - 1);
        us[bit] = 1 - us[bit];
        x = sdrCategoryEncoder.decode(us);
        assertEquals(x.getFields().get("bar").getDescription(), "US");

        // add two reps together
        newrep = ArrayUtils.or(us, es);
        x = sdrCategoryEncoder.decode(newrep);
        name = x.getFields().get("bar").getDescription();
        assertTrue("US ES".equals(name) || "ES US".equals(name));

        // Catch duplicate categories
        boolean caughtException = false;
        ArrayList<String> newCategories = new ArrayList<>(Arrays.asList(categories));
        newCategories.add("ES");
        try {
            sdrCategoryEncoder = SDRCategoryEncoder.builder()
                    .n(fieldWidth)
                    .w(bitsOn)
                    .categoryList(newCategories)
                    .name("foo")
                    .forced(true).build();
        } catch (IllegalArgumentException e) {
            caughtException = true;
        }
        if (!caughtException) {
            throw new RuntimeException("Did not catch duplicate category in constructor");
        }
    }

    @Test
    public void testAutoGrow() {
        //testing auto-grow
        int fieldWidth = 100;
        int bitsOn = 10;

        SDRCategoryEncoder sdrCategoryEncoder = SDRCategoryEncoder.builder()
                .n(fieldWidth)
                .w(bitsOn)
                .name("foo")
                .forced(true).build();
        int[] encoded = new int[fieldWidth];
        Arrays.fill(encoded, 0);
        assertEquals(sdrCategoryEncoder.topDownCompute(encoded).get(0).getValue(), "<UNKNOWN>");

        sdrCategoryEncoder.encodeIntoArray("catA", encoded);
        assertEquals(ArrayUtils.aggregateArray(encoded), bitsOn);
        assertEquals(sdrCategoryEncoder.getScalars("catA").get(0), 1.0, 0.0);
        int[] catA = new int[encoded.length];
        System.arraycopy(encoded, 0, catA, 0, encoded.length);

        sdrCategoryEncoder.encodeIntoArray("catB", encoded);
        assertEquals(ArrayUtils.aggregateArray(encoded), bitsOn);
        assertEquals(sdrCategoryEncoder.getScalars("catB").get(0), 2.0, 0.0);
        int[] catB = new int[encoded.length];
        System.arraycopy(encoded, 0, catB, 0, encoded.length);

        assertEquals(sdrCategoryEncoder.topDownCompute(catA).get(0).getValue(), "catA");
        assertEquals(sdrCategoryEncoder.topDownCompute(catB).get(0).getValue(), "catB");

        // empty field
        String[] emptyValues = {null, ""};
        for (String emptyValue : emptyValues) {
            sdrCategoryEncoder.encodeIntoArray(emptyValue, encoded);
            assertEquals(ArrayUtils.aggregateArray(encoded), 0);
            assertEquals(sdrCategoryEncoder.topDownCompute(encoded).get(0).getValue(), "<UNKNOWN>");
        }

        //Test Disabling Learning and autogrow
        sdrCategoryEncoder.setLearning(false);
        sdrCategoryEncoder.encodeIntoArray("catC", encoded);
        assertEquals(ArrayUtils.aggregateArray(encoded), bitsOn);
        assertEquals(sdrCategoryEncoder.getScalars("catC").get(0), 0, 0);
        assertEquals(sdrCategoryEncoder.topDownCompute(encoded).get(0).getValue(), "<UNKNOWN>");

        sdrCategoryEncoder.setLearning(true);
        sdrCategoryEncoder.encodeIntoArray("catC", encoded);
        assertEquals(ArrayUtils.aggregateArray(encoded), bitsOn);
        assertEquals(sdrCategoryEncoder.getScalars("catC").get(0), 3, 0);
        assertEquals(sdrCategoryEncoder.topDownCompute(encoded).get(0).getValue(), "catC");

    }

}
