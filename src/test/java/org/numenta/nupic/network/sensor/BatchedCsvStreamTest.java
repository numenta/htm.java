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
package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.BatchedCsvStream;
import org.numenta.nupic.network.sensor.BatchedCsvStream.BatchedCsvHeader;
import org.numenta.nupic.util.Tuple;


public class BatchedCsvStreamTest {
    public Stream<String> makeStream() {
        return Stream.of(
            "timestamp,consumption",
            "datetime,float",
            "T,",
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,16.4",
            "7/2/10 2:00,4.7",
            "7/2/10 3:00,4.7",
            "7/2/10 4:00,4.6",
            "7/2/10 5:00,23.5",
            "7/2/10 6:00,47.5",
            "7/2/10 7:00,45.4",
            "7/2/10 8:00,46.1",
            "7/2/10 9:00,41.5",
            "7/2/10 10:00,43.4",
            "7/2/10 11:00,43.8",
            "7/2/10 12:00,37.8",
            "7/2/10 13:00,36.6",
            "7/2/10 14:00,35.7",
            "7/2/10 15:00,38.9",
            "7/2/10 16:00,36.2",
            "7/2/10 17:00,36.6",
            "7/2/10 18:00,37.2",
            "7/2/10 19:00,38.2",
            "7/2/10 20:00,14.1");
    }
    
    /**
     * Returns a large test data set.
     * @return
     */
    public Stream<String> makeLargeStream() {
        final Path inputPath = Paths.get(ResourceLocator.path("rec-center-hourly.csv"));
        try {
            return Files.lines(inputPath);
        }catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    private boolean sequenceOutOfOrder = false;
    long lastSeqNum = 0;
    @Test
    public void testCanConfigParallelOperations() {
        boolean isParallel = false;
        BatchedCsvStream<String[]> csv = BatchedCsvStream.batch(makeLargeStream(), 2, isParallel, 3);
        
        csv.map(l -> { 
            assertTrue(l.getClass().isArray());
            assertTrue(Pattern.matches("^[0-9]+", l[0])); // Test for sequence number
            return l;
        }).count();
        assertFalse(csv.isBatchOp());
        
        // Assert that setting isParallel true results in a parallel operation by the BatchedCsvStream
        // by ensuring that the sequence numbers of each line are not all in sequence.
        isParallel = true;
        BatchedCsvStream<String[]> batchedStream = BatchedCsvStream.batch(makeLargeStream(), 20, isParallel, 3);
        Stream<String[]> stream = batchedStream.stream();
        // Assert batchOp status doesn't change until Stream actually starts batching
        assertFalse(batchedStream.isBatchOp());
        
        sequenceOutOfOrder = false;
        lastSeqNum = 0;
        // Altering state within a lambda like this is a strict no-no, but is done
        // here purely for test purposes.
        stream.mapToLong(l -> { 
            long lval = Long.parseLong(l[0]);
            sequenceOutOfOrder |= lval > lastSeqNum;
            lastSeqNum = lval;
            return Long.parseLong(l[0]);
            
        }).sum(); //Arbitrary operation to force stream termination and execution.
        assertTrue(sequenceOutOfOrder);
        assertTrue(batchedStream.isBatchOp());
    }
    
    @Test
    public void testHeaderFormation() {
        // Test that configured header size must be > 0
        int headerSize = 0;
        try {
            BatchedCsvStream.batch(makeLargeStream(), 2, true, headerSize);
            fail();
        }catch(Exception e) {
            assertEquals("Actual Header was not the expected size: > 1, but was: 0" , e.getMessage());
        }
        
        // Test that configured header size == 1, and that columns are sized at 2
        headerSize = 1;
        try {
            BatchedCsvStream<String[]> csv = BatchedCsvStream.batch(makeLargeStream(), 2, true, headerSize);
            BatchedCsvHeader header = csv.getHeader();
            assertEquals(headerSize, header.size());
            Tuple row = header.getRow(0);
            assertEquals(2, row.size());
        }catch(Exception e) {
            fail();
        }
        
        // Test that configured header size == 3
        headerSize = 3;
        try {
            BatchedCsvStream<String[]> csv = BatchedCsvStream.batch(makeLargeStream(), 2, true, headerSize);
            BatchedCsvHeader header = csv.getHeader();
            assertEquals(headerSize, header.size());
            Tuple row = header.getRow(0);
            assertEquals(2, row.size());
            row = header.getRow(1);
            assertEquals(2, row.size());
            row = header.getRow(2);
            assertEquals(2, row.size());
        }catch(Exception e) {
            fail();
        }
    }
    
    @Test
    public void testHeaderFormationWhenSynchronous() {
        try {
            int headerSize = 3;
            BatchedCsvStream<String[]> csv = BatchedCsvStream.batch(makeStream(), 2, false, headerSize);
            assertFalse(csv.isParallel());
            assertFalse(csv.stream().isParallel());
            BatchedCsvHeader header = csv.getHeader();
            assertEquals(headerSize, header.size());
        }catch(Exception e) {
            
        }
    }

}
