/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.network;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.BiFunction;

import org.junit.Ignore;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.serialize.HTMObjectInput;
import org.numenta.nupic.util.FastRandom;

import rx.Observer;


public class PlaygroundTest {
    private int[][] dayMap = new int[][] { 
        new int[] { 1, 1, 0, 0, 0, 0, 0, 1 },
        new int[] { 1, 1, 1, 0, 0, 0, 0, 0 },
        new int[] { 0, 1, 1, 1, 0, 0, 0, 0 },
        new int[] { 0, 0, 1, 1, 1, 0, 0, 0 },
        new int[] { 0, 0, 0, 1, 1, 1, 0, 0 },
        new int[] { 0, 0, 0, 0, 1, 1, 1, 0 },
        new int[] { 0, 0, 0, 0, 0, 1, 1, 1 },
    };
    
    private BiFunction<Inference, Integer, Integer> dayOfWeekPrintout = createDayOfWeekInferencePrintout();

    @Ignore
    public void testPlayground() {
        final int NUM_CYCLES = 600;
        final int INPUT_GROUP_COUNT = 7; // Days of Week
        
        ///////////////////////////////////////
        //          Load a Network           //
        ///////////////////////////////////////
        Network network = getLoadedDayOfWeekNetwork();
        
        int cellsPerCol = (int)network.getParameters().get(KEY.CELLS_PER_COLUMN);
        
        network.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @SuppressWarnings("unused")
            @Override
            public void onNext(Inference inf) {
                /** see {@link #createDayOfWeekInferencePrintout()} */
                int cycle = dayOfWeekPrintout.apply(inf, cellsPerCol);
            }
        });
        
        Publisher pub = network.getPublisher();
        
        network.start();
        
        int cycleCount = 0;
        for(;cycleCount < NUM_CYCLES;cycleCount++) {
            for(double j = 0;j < INPUT_GROUP_COUNT;j++) {
                pub.onNext("" + j);
            }
            
            network.reset();
        }
        
        // Test network output
        try {
            Region r1 = network.lookup("r1");
            r1.lookup("1").getLayerThread().join(2000);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
    ///////////////////////////////////////////
    //             HELPER METHODS            //
    ///////////////////////////////////////////
    private Network getLoadedDayOfWeekNetwork() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getDayDemoTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
                PublisherSupplier.builder()
                    .addHeader("dayOfWeek")
                    .addHeader("number")
                    .addHeader("B").build() }));
        
        Network network = Network.create("test network", p).add(Network.createRegion("r1")
            .add(Network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, true)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(sensor)));
        
        return network;
    }

    private BiFunction<Inference, Integer, Integer> createDayOfWeekInferencePrintout() {
        return new BiFunction<Inference, Integer, Integer>() {
            private int cycles = 1;
              
            public Integer apply(Inference inf, Integer cellsPerColumn) {
                Classification<Object> result = inf.getClassification("dayOfWeek");
                double day = mapToInputData((int[])inf.getLayerInput());
                if(day == 1.0) {
                    System.out.println("\n=========================");
                    System.out.println("CYCLE: " + cycles);
                    cycles++;
                }
                
                System.out.println("RECORD_NUM: " + inf.getRecordNum());
                System.out.println("ScalarEncoder Input = " + day);
                System.out.println("ScalarEncoder Output = " + Arrays.toString(inf.getEncoding()));
                System.out.println("SpatialPooler Output = " + Arrays.toString(inf.getFeedForwardActiveColumns()));
                
                if(inf.getPreviousPredictiveCells() != null)
                    System.out.println("TemporalMemory Previous Prediction = " + 
                        Arrays.toString(SDR.cellsAsColumnIndices(inf.getPreviousPredictiveCells(), cellsPerColumn)));
                
                System.out.println("TemporalMemory Actives = " + Arrays.toString(SDR.asColumnIndices(inf.getSDR(), cellsPerColumn)));
                
                System.out.print("CLAClassifier prediction = " + 
                    stringValue((Double)result.getMostProbableValue(1)) + " --> " + ((Double)result.getMostProbableValue(1)));
                
                System.out.println("  |  CLAClassifier 1 step prob = " + Arrays.toString(result.getStats(1)) + "\n");
                
                return cycles;
            }
        };
    }
    
    private double mapToInputData(int[] encoding) {
        for(int i = 0;i < dayMap.length;i++) {
            if(Arrays.equals(encoding, dayMap[i])) {
                return i + 1;
            }
        }
        return -1;
    }
    
    private String stringValue(Double valueIndex) {
        String recordOut = "";
        BigDecimal bdValue = new BigDecimal(valueIndex).setScale(3, RoundingMode.HALF_EVEN);
        switch(bdValue.intValue()) {
            case 1: recordOut = "Monday (1)";break;
            case 2: recordOut = "Tuesday (2)";break;
            case 3: recordOut = "Wednesday (3)";break;
            case 4: recordOut = "Thursday (4)";break;
            case 5: recordOut = "Friday (5)";break;
            case 6: recordOut = "Saturday (6)";break;
            case 0: recordOut = "Sunday (7)";break;
        }
        return recordOut;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T main(String[] args) throws Exception {
        InputStream input = new FileInputStream(new File("myfile"));
        //HTMObjectInput reader = Persistence.get().serializer().getObjectInput(input);
        try (HTMObjectInput reader = Persistence.get().serializer().getObjectInput(input)) {
            Class<?> aClass = null;//...  // Persistable subclass
            T t = (T) reader.readObject(aClass); // Where T is the Persistable subclass type (HTM.java object).
            return t;
        } catch(Exception e) {
            throw e;
        }
    }
}
