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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rx.Observer;

/**
 * Tests the structured process for building an {@link Observable}
 * emitter ({@code Publisher}) which can validate a manually constructed
 * input header and allow manual entry.
 * 
 * @author David Ray
 * @see Publisher
 * @see Header
 * @see ObservableSensor
 * @see ObservableSensorTest
 */
public class PublisherTest {

    @Test
    public void testHeaderConstructionAndManualEntry() {
        Publisher manual = Publisher.builder()
            .addHeader("timestamp,consumption")
            .addHeader("datetime,float")
            .addHeader("B")
            .build();
        
        final List<String> collected = new ArrayList<>();
        manual.subscribe(new Observer<String>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(String output) {
                collected.add(output);
            }
        });
        
        assertEquals(3, collected.size());
        
        String[] entries = { 
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,34.0",
            "7/2/10 2:00,40.4",
            "7/2/10 3:00,123.4",
        };
        
        for(String s : entries) {
            manual.onNext(s);
        }
        
        assertEquals(7, collected.size());
    }

    @Test
    public void testHeader() {
        try {
            Publisher.builder().build();
            fail();
        }catch(IllegalStateException e) {
            assertEquals("Header not properly formed (must contain 3 lines) see Header.java", e.getMessage());
        }

    }
}
