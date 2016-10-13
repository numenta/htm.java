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
package org.numenta.nupic;

import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_USE_MOVING_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;
import org.numenta.nupic.util.UniversalRandom;

public class RunLayer {
    public static boolean IS_VERBOSE = true;
    public static boolean LEARN = true;
    public static boolean TM_ONLY = true;
    public static boolean SP_ONLY = false;
    public static boolean NETWORK = false;
    
    public static class MakeshiftLayer {
        private Connections connections;
        private MultiEncoder encoder;
        private SpatialPooler sp;
        private TemporalMemory tm;
        private CLAClassifier classifier;
        @SuppressWarnings("unused")
        private Anomaly anomaly;
        
        private int recordNum;
        private int[] prevPredictedCols;
        private Map<String, Object> classification;
        
        private Network network;
        
//        private static String INPUT_PATH = "/Users/cogmission/git/NAB/data/artificialNoAnomaly/art_daily_no_noise.csv";
//        private static String readFile = "/Users/cogmission/git/NAB/data/artificialNoAnomaly/art_daily_sp_output.txt";
        private static String INPUT_PATH = "/Users/cogmission/git/NAB/data/realTraffic/TravelTime_387.csv";
        private static String readFile = "/Users/cogmission/git/newtm/htm.java/src/test/resources/TravelTime_sp_output.txt";
        
        private static List<int[]> input;
        private static List<String> raw;
        
        @SuppressWarnings("unused")
        private static String encFilePath = "/Users/cogmission/git/lscheinkman/NAB/art_daily_encoder_output_java.txt";
        private PrintWriter encFile = null;
        
        /**
         * Makeshift Layer to contain and operate on algorithmic entities
         * 
         * @param c         the {@link Connections} object.
         * @param encoder   the {@link MultiEncoder}
         * @param sp        the {@link SpatialPooler}
         * @param tm        the {@link TemporalMemory}
         * @param cl        the {@link CLAClassifier}
         */
        public MakeshiftLayer(Connections c, MultiEncoder encoder, SpatialPooler sp, 
            TemporalMemory tm, CLAClassifier cl, Anomaly anomaly) {
            
            this.connections = c;
            this.encoder = encoder;
            this.sp = sp;
            this.tm = tm;
            this.classifier = cl;
            
//            Parameters parameters = getParameters();
            // 2015-08-31 18:22:00,90
//            network = Network.create("NAB Network", parameters)
//                .add(Network.createRegion("NAB Region")
//                    .add(Network.createLayer("NAB Layer", parameters)
//                        .add(Anomaly.create())
//                        .add(new TemporalMemory())));
//            
//            network.observe().subscribe((inference) -> {
//                double score = inference.getAnomalyScore();
//                int record = inference.getRecordNum();
//                
//                recordNum = record;
//                
//                printHeader();
//                
//                Set<Cell> act = ((ManualInput)inference).getActiveCells();
//                int[] activeColumnIndices = SDR.cellsAsColumnIndices(act, connections.getCellsPerColumn());
//                Set<Cell> prev = ((ManualInput)inference).getPreviousPredictiveCells();
//                int[] prevPredColumnIndices = prev == null ? null : SDR.cellsAsColumnIndices(prev, connections.getCellsPerColumn());
//                String input = Arrays.toString((int[])((ManualInput)inference).getLayerInput());
//                String prevPred = prevPredColumnIndices == null ? "null" : Arrays.toString(prevPredColumnIndices);
//                String active = Arrays.toString(activeColumnIndices);
//                System.out.println("          TemporalMemory Input: " + input);
//                System.out.println("TemporalMemory prev. predicted: " + prevPred);
//                System.out.println("         TemporalMemory active: " + active);
//                System.out.println("Anomaly Score: " + score + "\n");
//                
//            }, (error) -> {
//                error.printStackTrace();
//            }, () -> {
//                // On Complete
//            });
        }
        
        public void printHeader() {
            System.out.println("--------------------------------------------");
            System.out.println("Record #: " + recordNum + "\n");
            System.out.println("Raw Input: " + MakeshiftLayer.raw.get(recordNum + 1));
        }
        
        /**
         * Input through Encoder
         * 
         * @param timestamp
         * @param value
         * @param isVerbose
         * @return Tuple { encoding, bucket index }
         */
        Encoder<?> valueEncoder = null;
        public Tuple encodingStep(DateTime timestamp, double value, boolean isVerbose) {
            if(valueEncoder == null) {
                List<EncoderTuple> encoderTuples = encoder.getEncoders(encoder);
                valueEncoder = encoderTuples.get(0).getEncoder(); 
            }
            
            Map<String, Object> encodingInput = new HashMap<String, Object>();
            encodingInput.put("value", value);
            encodingInput.put("timestamp",  timestamp);
            int[] encoding = encoder.encode(encodingInput);
            if(isVerbose) {
                System.out.println("ScalarEncoder Output = " + Arrays.toString(encoding));
            }
            
            int bucketIdx = valueEncoder.getBucketIndices((double)value)[0];
//            writeEncOutput(encoding);
            return new Tuple(encoding, bucketIdx);
        }
        
        /**
         * Input through the spatial pooler 
         * 
         * @param encoding
         * @param isVerbose
         * @return Tuple { dense output, sparse output }
         */
        public Tuple spStep(int[] encoding, boolean learn, boolean isVerbose) {
            // Input through Spatial Pooler
            int[] output = new int[connections.getNumColumns()];
            sp.compute(connections, encoding, output, true);
            int[] sparseSPOutput = ArrayUtils.where(output, ArrayUtils.WHERE_1);
            if(isVerbose) {
                System.out.println("SpatialPooler Output = " + Arrays.toString(sparseSPOutput) + "\n");
            }
            
            return new Tuple(output, sparseSPOutput);
        }
        
        public void networkStep(int[] sparseSPOutput, boolean learn) {
            network.compute(sparseSPOutput);
        }
        
        /**
         * Input through the Temporal Memory
         * @param sparseSPOutput
         * @param learn
         * @param isVerbose
         * @return Tuple { active cell indices, previous predicted column indices, predicted column indices }
         */
        public Tuple tmStep(int[] sparseSPOutput, boolean learn, boolean isVerbose) {
            // Input into the Temporal Memory
            ComputeCycle cc = tm.compute(connections, sparseSPOutput, learn);
            int[] activeCellIndices = cc.activeCells().stream().mapToInt(c -> c.getIndex()).sorted().toArray();
            int[] predColumnIndices = SDR.cellsAsColumnIndices(cc.predictiveCells(), connections.getCellsPerColumn());
            int[] activeColumns = Arrays.stream(activeCellIndices)
                .map(cell -> cell / connections.getCellsPerColumn())
                .distinct()
                .sorted()
                .toArray();
            if(isVerbose) {
                System.out.println("          TemporalMemory Input: " + Arrays.toString(sparseSPOutput));
                System.out.println("TemporalMemory prev. predicted: " + Arrays.toString(prevPredictedCols));
                System.out.println("         TemporalMemory active: " + Arrays.toString(activeColumns));
            }
            
            return new Tuple(activeCellIndices, prevPredictedCols, predColumnIndices);
        }
        
        /**
         * Run classification step
         * 
         * @param activeCellIndices
         * @param bucketIdx
         * @param inputValue
         * @param learn
         * @param isVerbose
         * @return  the {@link Classification}
         */
        public Classification<Double> classificationStep(int[] activeCellIndices, int bucketIdx, Object inputValue, boolean learn, boolean isVerbose) {
            classification.put("bucketIdx", bucketIdx);
            classification.put("actValue", inputValue);
            
            Classification<Double> result = classifier.compute(recordNum, classification, activeCellIndices, true, true);
            if(isVerbose) {
                System.out.print("CLAClassifier Prediction = " + result.getMostProbableValue(1));
                System.out.println("  |  CLAClassifier 1 step prob = " + Arrays.toString(result.getStats(1)) + "\n");
            }
            
            return result;
        }
        
        /**
         * Run Anomaly computer step
         * @param sparseSPOutput
         * @param prevPredictedCols
         * @param isVerbose
         * @return  the raw anomaly score
         */
        public double anomalyStep(int[] sparseSPOutput, int[] prevPredictedCols, boolean isVerbose) {
            double anomalyScore = Anomaly.computeRawAnomalyScore(sparseSPOutput, prevPredictedCols);
            if(isVerbose) {
                System.out.println("Anomaly Score: " + anomalyScore + "\n");
            }
            
            return anomalyScore;
        }
        
        /**
         * Increment the record number
         */
        public void incRecordNum() {
            recordNum++;
        }
        
        /**
         * Returns the current record number
         * @return
         */
        public int getRecordNum() {
            return recordNum;
        }
        
        /**
         * At the end of a cycle, stores the current prediction
         * as the previous prediction.
         * 
         * @param predColumnIndices
         */
        public void storeCyclePrediction(int[] predColumnIndices) {
            prevPredictedCols = predColumnIndices;
        }
        
        public void writeEncOutput(int[] output) {
            try {
                encFile.println(Arrays.toString(output));
            }catch(Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    /////////////////////////////////// End Layer Class Definition ////////////////////////////////////////
    
    public static MultiEncoder createEncoder() {
        MultiEncoder encoder = MultiEncoder.builder().name("").build();
        ScalarEncoder se = ScalarEncoder.builder()
            .n(50)
            .w(21)
            .minVal(0)
            .maxVal(100)
            .periodic(false)
            .clipInput(true)
            .name("value")
            .build();
        encoder.addEncoder("value", se);
        
        DateEncoder de = DateEncoder.builder()
            .timeOfDay(21, 9.5)
            .name("timestamp")
            .build();
        encoder.addEncoder("timestamp", de);
        
        return encoder;
    }
    
    public static Parameters getParameters() {
        UniversalRandom random = new UniversalRandom(42);
        
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 104 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        parameters.set(KEY.CELLS_PER_COLUMN, 32);
        parameters.set(KEY.RANDOM, random);

        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.85);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, true);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.0015);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 1000);
        parameters.set(KEY.MAX_BOOST, 2.0);
        
        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.set(KEY.MIN_THRESHOLD, 4);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.1);//0.05
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.1);//0.05
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);
        
        return parameters;
    }
    
    public static MakeshiftLayer createLayer() {
        MultiEncoder encoder = createEncoder();
        
        Parameters parameters = getParameters();
        
        //////////////////////////////////////////////////////////
//        testRandom(random);
        //////////////////////////////////////////////////////////
        
        //////////////////////////////////////////////////////////
//        int[] encoding = testEncoder(encoder);
        //////////////////////////////////////////////////////////
        
        Connections conn = new Connections();
        parameters.apply(conn);
        
        SpatialPooler sp = null;
        if(!RunLayer.TM_ONLY) {
            sp = new SpatialPooler();
            sp.init(conn);
        }
        
        //////////////////////////////////////////////////////////
//        int[] sparseSdr = testSpatialPooler(sp, conn, encoding);
        //////////////////////////////////////////////////////////
        TemporalMemory tm = null;
        if(!RunLayer.SP_ONLY) {
            tm = new TemporalMemory();
            TemporalMemory.init(conn);
        }
        
        //////////////////////////////////////////////////////////
        // Tuple = (activeCells, predictiveCells)
//        testTemporalMemory(tm, conn, sparseSdr);
        //////////////////////////////////////////////////////////
        
        //CLAClassifier cl = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.3, 0);
        
        //////////////////////////////////////////////////////////
        // Test Classifier Output
        // ...
        //////////////////////////////////////////////////////////
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        params.put(KEY_WINDOW_SIZE, 0);
        params.put(KEY_USE_MOVING_AVG, false);
        
        Anomaly anomalyComputer = Anomaly.create(params);
        
        //////////////////////////////////////////////////////////
        // Test Anomaly Output
        // ...
        //////////////////////////////////////////////////////////
        
        MakeshiftLayer layer = new MakeshiftLayer(conn, encoder, sp, tm, null, anomalyComputer); 
        return layer;
    }
    
    public static void printPreliminaryTestHeader() {
        System.out.println("--------------------------------------------");
        System.out.println("Prelimary Test of: Random, Encoder, SP, TM, Classifier, Anomaly: \n");
    }
    
    public static void testRandom(Random random) {
        for(int i = 0;i < 10;i++) {
            System.out.println("random[" + i + "] = " + random.nextInt());
        }
        
        for(int i = 0;i < 10;i++) {
            System.out.println("randomDbl[" + i + "] = " + random.nextDouble());
        }
    }
    
    public static int[] testEncoder(MultiEncoder encoder) {
        DateTime timestamp = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime("2014-04-01 00:00:00");
        double value = 20.0;
        
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("value", value);
        d.put("timestamp",  timestamp);
        int[] output = encoder.encode(d);
        System.out.println("ScalarEncoder Output = " + Arrays.toString(output));
        System.out.println("len = " + output.length);
        
        return output;
    }
    
    public static int[] testSpatialPooler(SpatialPooler sp, Connections conn, int[] encoding) {
        int[] dense = new int[2048];
        //sp.compute(conn, encoding, dense, true, true);
        int[] sparse = ArrayUtils.where(dense, ArrayUtils.WHERE_1);
        System.out.println("SP out = " + Arrays.toString(sparse));
        System.out.println("SP out len: " + sparse.length);
        System.out.println("");
        
        return sparse;
    }
    
    public static Tuple testTemporalMemory(TemporalMemory tm, Connections conn, int[] sparseSPOutput) {
        int[] expected = { 
                0, 87, 96, 128, 145, 151, 163, 180, 183, 218, 233, 242, 250, 260, 
                264, 289, 290, 303, 312, 313, 334, 335, 337, 342, 346, 347, 353, 355, 356, 357, 
                365, 370, 376, 384, 385, 391, 392, 393, 408, 481 };
        System.out.println("First SP Output Line == expected ? " + Arrays.equals(sparseSPOutput, expected));
        
        ComputeCycle cc = tm.compute(conn, sparseSPOutput, true);
        System.out.println("TM act out: " + Arrays.toString(SDR.asCellIndices(cc.activeCells())));
        System.out.println("TM act out: " + Arrays.toString(SDR.asCellIndices(cc.predictiveCells())));
        
        return new Tuple(cc.activeCells, cc.predictiveCells());
    }
    
    public static void loadSPOutputFile() {
        try (Stream<String> stream = Files.lines(Paths.get(MakeshiftLayer.readFile))) {
            MakeshiftLayer.input = stream.map(l -> {
                String line = l.replace("[", "").replace("]",  "").trim();
                int[] result = Arrays.stream(line.split("[\\s]*\\,[\\s]*")).mapToInt(i -> Integer.parseInt(i)).toArray();
                return result;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void loadRawInputFile() {
        try (Stream<String> stream = Files.lines(Paths.get(MakeshiftLayer.INPUT_PATH))) {
            MakeshiftLayer.raw = stream.map(l -> l.trim()).collect(Collectors.toList());
        }catch(Exception e) {e.printStackTrace();}
    }
    
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        
        RunLayer.MakeshiftLayer layer = RunLayer.createLayer();
        
       System.out.println("\n===================================\n");
        
        loadSPOutputFile();
        loadRawInputFile();
        if(NETWORK) {
            for(int i = 0;i < MakeshiftLayer.input.size();i++) {
                int[] sparseSPOutput = MakeshiftLayer.input.get(i);
                layer.networkStep(sparseSPOutput, LEARN);
            }
        }else if(TM_ONLY) {
            for(int i = 0;i < MakeshiftLayer.input.size();i++) {
                layer.printHeader();
                int[] sparseSPOutput = MakeshiftLayer.input.get(i);
                Tuple tmTuple = layer.tmStep(sparseSPOutput, LEARN, IS_VERBOSE);
                double score = layer.anomalyStep(sparseSPOutput, (int[])tmTuple.get(1), true);
                layer.incRecordNum();
                
                // Store the current prediction as previous
                layer.storeCyclePrediction((int[])tmTuple.get(2));
            }
        }else if(SP_ONLY) {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            try (Stream<String> stream = Files.lines(Paths.get(MakeshiftLayer.INPUT_PATH))) {
                stream.skip(1).forEach(l -> {
                    String[] line = l.split("[\\s]*\\,[\\s]*");
                    
                    DateTime timestamp = formatter.parseDateTime(line[0].trim());
                    double value = Double.parseDouble(line[1].trim());
                    layer.printHeader();
                    Tuple encTuple = layer.encodingStep(timestamp, value, IS_VERBOSE);
                    Tuple spTuple = layer.spStep((int[])encTuple.get(0), LEARN, IS_VERBOSE);
                    
                    layer.incRecordNum();
                    if(layer.recordNum == 200) {
                        System.exit(0);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            try (Stream<String> stream = Files.lines(Paths.get(MakeshiftLayer.INPUT_PATH))) {
                stream.skip(1).forEach(l -> {
                    String[] line = l.split("[\\s]*\\,[\\s]*");
                    DateTime timestamp = formatter.parseDateTime(line[0].trim());
                    double value = Double.parseDouble(line[1].trim());
                    layer.printHeader();
                    Tuple encTuple = layer.encodingStep(timestamp, value, IS_VERBOSE);
                    Tuple spTuple = layer.spStep((int[])encTuple.get(0), LEARN, IS_VERBOSE);
                    int[] spOutput = (int[])spTuple.get(1);
                    Tuple tmTuple = layer.tmStep((int[])spTuple.get(1), LEARN, IS_VERBOSE); 
                    //Classification<Double> cla = layer.classificationStep((int[])tmTuple.get(0), (int)encTuple.get(1), value, LEARN, IS_VERBOSE);
                    double score = layer.anomalyStep(spOutput, layer.prevPredictedCols, IS_VERBOSE);
                
                    layer.incRecordNum();
                    
                    // Store the current prediction as previous
                    layer.storeCyclePrediction((int[])tmTuple.get(2));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("--- " + ((System.currentTimeMillis() - start) / 1000d) + " seconds ---");
    }

}
