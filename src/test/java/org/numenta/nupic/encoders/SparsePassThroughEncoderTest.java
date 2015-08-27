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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

public class SparsePassThroughEncoderTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testEncodeArray_24outputBits() {
        SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(24, 5);
        encoder.setName("foo");

        //Send bitmap as array of indices
        int bitmap[] = {2, 7, 15, 18, 23};
        int output[] = new int[24];
        encoder.encodeIntoArray(bitmap, output);
        assertEquals(bitmap.length, ArrayUtils.sum(output));
        Tuple decode = encoder.decode(output, null);
        assertTrue(((HashMap<String, RangeList>) decode.get(0)).containsKey(encoder.getName()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEncodeArray_12outputBits() {
        SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(12, 2);
        encoder.setName("foo2");

        //Send bitmap as array of indices
        int bitmap[] = {0, 11};
        int output[] = new int[12];
        encoder.encodeIntoArray(bitmap, output);
        assertEquals(bitmap.length, ArrayUtils.sum(output));
        Tuple decode = encoder.decode(output, null);
        assertTrue(((HashMap<String, RangeList>) decode.get(0)).containsKey(encoder.getName()));
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testArrayInvalidWTooBig() {
        SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(9, 3);
        exception.expect(IllegalArgumentException.class);
        encoder.encode(new int[]{2});

        encoder = SparsePassThroughEncoder.sparseBuilder()
                .n(9)
                .w(3)
                .name("foo")
                .build();
        exception.expect(IllegalArgumentException.class);
        encoder.encode(new int[]{2});
    }

    @Test
    public void testArrayInvalidWTooSmall() {
        SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(9, 3);
        exception.expect(IllegalArgumentException.class);
        encoder.encode(new int[]{2, 7, 15, 18, 23});

        encoder = SparsePassThroughEncoder.sparseBuilder()
                .n(9)
                .w(3)
                .name("foo")
                .build();
        exception.expect(IllegalArgumentException.class);
        encoder.encode(new int[]{2, 7, 15, 18, 23});
    }

    @Ignore
    private void testCloseInner(int[] bitmap1, int outputWidth1, int[] bitmap2, int outputWidth2, double expectedScore) {
        SparsePassThroughEncoder encoder1 = new SparsePassThroughEncoder(outputWidth1, ArrayUtils.where(bitmap1, ArrayUtils.GREATER_OR_EQUAL_0).length);
        SparsePassThroughEncoder encoder2 = new SparsePassThroughEncoder(outputWidth2, ArrayUtils.where(bitmap2, ArrayUtils.GREATER_OR_EQUAL_0).length);

        int[] out1 = encoder1.encode(bitmap1);
        int[] out2 = encoder2.encode(bitmap2);

        TDoubleList result = encoder1.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
        assertTrue(result.size() == 1);
        assertEquals(expectedScore, result.get(0), 0.0);

        encoder1 = SparsePassThroughEncoder.sparseBuilder()
                .n(outputWidth1)
                .w(ArrayUtils.where(bitmap1, ArrayUtils.GREATER_OR_EQUAL_0).length)
                .build();
        encoder2 = SparsePassThroughEncoder.sparseBuilder()
                .n(outputWidth2)
                .w(ArrayUtils.where(bitmap2, ArrayUtils.GREATER_OR_EQUAL_0).length)
                .build();

        out1 = encoder1.encode(bitmap1);
        out2 = encoder2.encode(bitmap2);

        result = encoder1.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
        assertTrue(result.size() == 1);
        assertEquals(expectedScore, result.get(0), 0.0);
    }

    @Test
    public void testClosenessScores() {
        //Identical => 1
        testCloseInner(new int[]{2, 7, 15, 18, 23}, 24, new int[]{2, 7, 15, 18, 23}, 24, 1.0);

        //No overlap => 0
        testCloseInner(new int[]{2, 7, 15, 18, 23}, 24, new int[]{3, 9, 14, 19, 24}, 25, .0);
        //Similar => 4 of 5 match
        testCloseInner(new int[]{2, 7, 15, 18, 23}, 24, new int[]{2, 7, 17, 18, 23}, 24, .8);

        //Little => 1 of 5 match
        testCloseInner(new int[]{2, 7, 15, 18, 23}, 24, new int[]{3, 7, 17, 19, 24}, 25, .2);

        //Extra active bit => off by 1 of 5
        testCloseInner(new int[]{2, 7, 15, 18, 23}, 24, new int[]{2, 7, 11, 15, 18, 23}, 24, .8);

        //Missing active bit => off by 1 of 5
        testCloseInner(new int[]{2, 7, 15, 18, 23}, 24, new int[]{2, 7, 18, 23}, 24, .8);
    }
}
