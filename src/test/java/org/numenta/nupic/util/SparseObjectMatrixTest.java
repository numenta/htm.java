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

package org.numenta.nupic.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SparseObjectMatrixTest {

    @Test
    public void testGetDimensionMultiples() {
        SparseMatrix<TestObject> sm = new SparseObjectMatrix<TestObject>(new int[] { 4 });
        int[] dm = sm.getDimensionMultiples();
        assertEquals(1, dm.length);
        assertEquals(1, dm[0]);

        sm = new SparseObjectMatrix<TestObject>(new int[] { 1, 2, 3, 4, 5 });
        dm = sm.getDimensionMultiples();
        assertEquals(ArrayUtils.print1DArray(dm), "[120, 60, 20, 5, 1]");
    }

    /**
     * Test that indices are sparse, that they are an expected number and that
     * they are in ascending order.
     */
    @Test
    public void testGetSparseIndices() {
        SparseObjectMatrix<TestObject> sm = new SparseObjectMatrix<TestObject>(new int[] { 32 });
        sm.set(0, new TestObject(1, 1));
        sm.set(2, new TestObject(2, 2));
        sm.set(1, new TestObject(3, 3));
        sm.set(1, new TestObject(4, 4));
        sm.set(3, new TestObject(5, 5));
        int[] indices = sm.getSparseIndices();
        assertEquals(4, indices.length);
        assertEquals(indices[0], 0);
        assertEquals(indices[1], 1);
        assertEquals(indices[2], 2);
        assertEquals(indices[3], 3);
    }

    @Test
    public void testGetMaxIndex() {
        SparseObjectMatrix<TestObject> sm = 
                new SparseObjectMatrix<TestObject>(new int[] { 5, 5 });

        int max = sm.getMaxIndex();
        assertEquals(24, max);
    }

    /**
     * Test that a multidimensional array may be specified, and that they
     * may be filled with objects created by a specified {@link TypeFactory}
     */
    @Test
    public void testAsDense() {
        SparseObjectMatrix<TestObject> sm = new SparseObjectMatrix<TestObject>(new int[] { 2, 32, 32 });//2048
        TypeFactory<TestObject> f = new TypeFactory<TestObject>() {
            int index = 0;
            @Override public TestObject make(int... args) {
                return new TestObject(32, index++); // #Cells, Cell index
            }
            @Override public Class<TestObject> typeClass() {
                return TestObject.class;
            }
        };

        Object o = sm.asDense(f);
        TestObject[][][] cast = (TestObject[][][])o;

        assertEquals(cast.length, 2);
        assertEquals(cast[0].length, 32);
        assertEquals(cast[0][0].length, 32);
    }

    /**
     * Compute the equivalent flat index
     */
    @Test
    public void testComputeIndex() {
        SparseMatrix<TestObject> l = new SparseObjectMatrix<TestObject>(new int[] { 2, 4, 4 });

        int index = l.computeIndex(new int[] { 0, 2, 2 });
        assertEquals(10, index);

        index = l.computeIndex(new int[] { 1, 2, 3 });
        assertEquals(27, index);
    }

    /**
     * Compute the equivalent flat index using column major indexing
     */
    @Test
    public void testComputeIndex_ColumnMajor() {
        // Column major
        SparseMatrix<TestObject> l = new SparseObjectMatrix<TestObject>(new int[] { 4, 4, 2 }, true);

        int index = l.computeIndex(new int[] { 2, 2, 0 });
        assertEquals(10, index);

        index = l.computeIndex(new int[] { 3, 2, 1 });
        assertEquals(27, index);
    }

    /**
     * Print the multidimensional index from a given flat index.
     */
    @Test
    public void testComputeCoordinates() {
        SparseMatrix<TestObject> l = new SparseObjectMatrix<TestObject>(new int[] { 2, 4, 4 });
        int[] coordinates = l.computeCoordinates(27);
        assertEquals(1, coordinates[0]);
        assertEquals(2, coordinates[1]);
        assertEquals(3, coordinates[2]);
    }

    /**
     * Print the multidimensional column major index from a given flat index.
     */
    @Test
    public void testComputeCoordinates_ColumnMajor() {
        SparseMatrix<TestObject> l = new SparseObjectMatrix<TestObject>(new int[] { 4, 4, 2 }, true);
        int[] coordinates = l.computeCoordinates(27);
        assertEquals(3, coordinates[0]);
        assertEquals(2, coordinates[1]);
        assertEquals(1, coordinates[2]);
    }

    @Test 
    public void testSetAndGet() {
        int[] dimensions =  { 5, 2 };
        TestObject[][] values = new TestObject[dimensions[0]][dimensions[1]];

        for (int i = 0; i < dimensions[0]; i ++)
            for (int j = 0; j < dimensions[1]; j++)
                values[i][j] = new TestObject(i, j);

        SparseObjectMatrix<TestObject> som = new SparseObjectMatrix<TestObject>(dimensions);

        for (int i = 0; i < dimensions[0]; i ++)
            for (int j = 0; j < dimensions[1]; j++)
                som.set(new int[] {i, j} , values[i][j]);

        for (int i = 0; i < dimensions[0]; i ++) {
            for (int j = 0; j < dimensions[1]; j++) {
                int[] indexes = { i, j };
                assertArrayEquals(indexes, som.get(indexes).getArgs());
            }
        }
    }

    /**
     * Test object for instantiation tests
     */
    public class TestObject {
        private int arg0;
        private int arg1;
        public TestObject(int arg0, int arg1) {
            this.arg0 = arg0;
            this.arg1 = arg1;
        }
        public int getArg0() { return arg0; }
        public int getArg1() { return arg1; }
        public int[] getArgs() { return new int[] {arg0, arg1}; }
    }
}
