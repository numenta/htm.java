package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.model.SDR;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.FastRandom;
import org.numenta.nupic.util.UniversalRandom;

import com.cedarsoftware.util.DeepEquals;

import rx.Observer;

/**
 * This file contains two test methods:
 * <ul>
 *  <li>{@link #testSimpleLayer()} - which outputs data processed by the raw assembly of algorithms. and...
 *  <li>{@link #testNetworkAPI() - which outputs data processed the Network API (NAPI)
 * </ul>
 * 
 * As a sort of "vetting" of the NAPI, this illustrates that the resultant 
 * output is <b><i>exactly</i></b> the same proving that the NAPI does not 
 * impact the actual results. (at the time of this writing: 07/21/2016)
 * 
 * @author cogmission
 */
public class NetworkConsistencyTest {
    private static final int UPPER_BOUNDARY = 8;
    private static final int RECORDS_PER_CYCLE = 7;
    
    private static Set<SampleWeek> simpleSamples = new HashSet<>();
    private static Set<SampleWeek> napiSamples = new HashSet<>();
    
    private static boolean doPrintout = false;
    
    private static final int SAMPLE_WEEK = new UniversalRandom(42).nextInt(125);
    
    @AfterClass
    public static void compare() {
        System.out.println("USING SAMPLE #: " + SAMPLE_WEEK);
        assertEquals(napiSamples.size(), simpleSamples.size());
        
        if(doPrintout) {
            System.out.println("\n--------------------------------");
            for(Iterator<SampleWeek> it = simpleSamples.iterator(), it2 = napiSamples.iterator();it.hasNext() && it2.hasNext();) {
                SampleWeek sw1 = it.next();
                SampleWeek sw2 = it2.next();
                System.out.println("Seq#: " + sw1.seqNum + " - " + sw2.seqNum);
                System.out.println("Encoder: " + Arrays.toString(sw1.encoderOut) + " - " + Arrays.toString(sw2.encoderOut));
                System.out.println("SP: " + Arrays.toString(sw1.spOut) + " - " + Arrays.toString(sw2.spOut));
                System.out.println("TM (in): " + Arrays.toString(sw1.tmIn) + " - " + Arrays.toString(sw2.tmIn));
                System.out.println("TM (pred. cols): " + Arrays.toString(sw1.tmPred) + " - " + Arrays.toString(sw2.tmPred));
                System.out.println("TM (Active Cells): " + Arrays.toString(sw1.activeCells) + " - " + Arrays.toString(sw2.activeCells));
                System.out.println("TM (Predictive Cells): " + Arrays.toString(sw1.predictiveCells) + " - " + Arrays.toString(sw2.predictiveCells));
                System.out.println("Anomaly Score: " + sw1.score + " - " + sw2.score);
                System.out.println("");
            }
        }
        assertTrue(DeepEquals.deepEquals(simpleSamples, napiSamples));
    }

    ////////////////////////////////////////////
    //          JUnit Test Methods            //
    ////////////////////////////////////////////
    /**
     * Test the "raw" assembly of algorithms using a makeshift
     * "faux" layer container.
     */
    @Test
    public void testSimpleLayer() {
        SimpleLayer layer = new SimpleLayer();
        
        for(int i = 0;i < 200;i++) {
            for(int j = 1;j < UPPER_BOUNDARY;j++) {
                layer.input(j, i, RECORDS_PER_CYCLE * i + j);
            }
        }
    }
    
    /**
     * Test an assembly which is the same as the above using
     * HTM.Java's Network API.
     */
    @Test
    public void testNetworkAPI() {
        Network network = getNetwork();
        
        network.start();
        
        Publisher publisher = network.getPublisher();
        
        for(int i = 0;i < 200;i++) {
            for(int j = 1;j < UPPER_BOUNDARY;j++) {
                publisher.onNext(String.valueOf(j));
            }
        }
        
        publisher.onComplete();
        
        try {
            Region r = network.lookup("NAB Region");
            r.lookup("NAB Layer").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Rudimentary test of the anomaly computation.
     */
    @Test
    public void testComputeAnomaly_4of6() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[] { 2, 5, 6, 11, 14, 18 }, new int[] { 2, 6, 11, 14 }, 0, 0);
        assertEquals(0.3333333333333333, score, 0);
    }
    
    /**
     * Rudimentary test of the anomaly computation.
     */
    @Test
    public void testComputeAnomaly_5of7() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);
        Anomaly anomalyComputer = Anomaly.create(params);
        double score = anomalyComputer.compute(new int[] { 0, 1, 8, 10, 13, 16, 18 }, new int[] { 0, 10, 13, 16, 18 }, 0, 0);
        assertEquals(0.2857142857142857, score, 0);
    }
    
 //--------------------------------------------------------------------------------------
    
    ////////////////////////////////////////////
    //            Support Methods             //
    ////////////////////////////////////////////
    private Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.set(KEY.CELLS_PER_COLUMN, 6);

        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.0015);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        
        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.set(KEY.MIN_THRESHOLD, 5);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.1);//0.05
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.1);//0.05
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);
        
        parameters.set(KEY.RANDOM, new FastRandom(42));

        return parameters;
    }
    
    /**
     * Parameters and meta information for the "dayOfWeek" encoder
     * @return
     */
    public Map<String, Map<String, Object>> getDayDemoFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                8, // n
                3, // w
                1.0, 8.0, 1, 1, Boolean.TRUE, null, Boolean.TRUE,
                "dayOfWeek", "number", "ScalarEncoder");
        return fieldEncodings;
    }
    
    public static Map<String, Map<String, Object>> setupMap(
        Map<String, Map<String, Object>> map,
        int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
        Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {

        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }
    
        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);
    
        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clipInput", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);
    
        return map;
    }
    
    public String stringValue(Double valueIndex) {
        String recordOut = "";
        BigDecimal bdValue = new BigDecimal(valueIndex).setScale(3, RoundingMode.HALF_EVEN);
        switch(bdValue.intValue()) {
            case 1: recordOut = "Monday (1)";break;
            case 2: recordOut = "Tuesday (2)";break;
            case 3: recordOut = "Wednesday (3)";break;
            case 4: recordOut = "Thursday (4)";break;
            case 5: recordOut = "Friday (5)";break;
            case 6: recordOut = "Saturday (6)";break;
            case 7: recordOut = "Sunday (7)";break;
        }
        return recordOut;
    }
    
    private Network getNetwork() {
        // Create Sensor publisher to push NAB input data to network
        PublisherSupplier supplier = PublisherSupplier.builder()
            .addHeader("dayOfWeek")
            .addHeader("number")
            .addHeader("B").build();

        // Get updated model parameters
        Parameters parameters = getParameters();
        Map<String, Map<String, Object>> fieldEncodings = getDayDemoFieldEncodingMap();
        parameters.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        
        int cellsPerColumn = (int)parameters.get(KEY.CELLS_PER_COLUMN);
        
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.PURE);

        // Create NAB Network
        Network network = Network.create("NAB Network", parameters)
            .add(Network.createRegion("NAB Region")
                .add(Network.createLayer("NAB Layer", parameters)
                    .add(Anomaly.create(params))
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(ObservableSensor::create,
                            SensorParams.create(SensorParams.Keys::obs, "Manual Input", supplier)))));
        
        network.observe().subscribe(new Observer<Inference>() { 
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override
            public void onNext(Inference inf) {
                String layerInput = inf.getLayerInput().toString();
                
                if(inf.getRecordNum() % RECORDS_PER_CYCLE == 0 && doPrintout) {
                    System.out.println("--------------------------------------------------------");
                    System.out.println("Iteration: " + (inf.getRecordNum() / 7));
                }
                if(doPrintout) System.out.println("===== " + layerInput + "  - Sequence Num: " + (inf.getRecordNum() + 1) + " =====");
                
                if(doPrintout) System.out.println("ScalarEncoder Input = " + layerInput);
                if(doPrintout) System.out.println("ScalarEncoder Output = " + Arrays.toString(inf.getEncoding()));
                
                if(doPrintout) System.out.println("SpatialPooler Output = " + Arrays.toString(inf.getFeedForwardActiveColumns()));
                int[] predictedColumns = SDR.cellsAsColumnIndices(inf.getPredictiveCells(), cellsPerColumn); //Get the predicted column indexes
                if(doPrintout) System.out.println("TemporalMemory Input = " + Arrays.toString(inf.getFeedForwardSparseActives()));
                if(doPrintout) System.out.println("TemporalMemory Prediction = " + Arrays.toString(predictedColumns));
                Set<Cell> actives = inf.getActiveCells();
                int[] actCellIndices = SDR.asCellIndices(actives);
                if(doPrintout) System.out.println("TemporalMemory Active Cells = " + Arrays.toString(actCellIndices));
                Set<Cell> pred = inf.getPredictiveCells();
                int[] predCellIndices = SDR.asCellIndices(pred);
                if(doPrintout) System.out.println("TemporalMemory Predictive Cells = " + Arrays.toString(predCellIndices));
                
                //Anomaly 
                double score = inf.getAnomalyScore();
                if(doPrintout) System.out.println("Anomaly Score = " + score);
                
                if(inf.getRecordNum() / 7 == SAMPLE_WEEK) {
                    napiSamples.add(new SampleWeek(inf.getRecordNum() + 1, inf.getEncoding(), inf.getFeedForwardActiveColumns(), 
                        inf.getFeedForwardSparseActives(), predictedColumns, actCellIndices, predCellIndices, score));
                }
            }
        });
        
        return network;
    }
    
    
//---------------------------------------------------------------------------------------
    
    /////////////////////////////////////
    //        A Simple Layer Class     //
    /////////////////////////////////////
    
    class SimpleLayer {
        private Parameters params;

        private Connections memory = new Connections();

        private ScalarEncoder encoder;
        private SpatialPooler spatialPooler;
        private TemporalMemory temporalMemory;
        private Anomaly anomaly;
        
        private int columnCount;
        private int cellsPerColumn;
        
        private int[] predictedColumns;
        private int[] prevPredictedColumns;
        
        
        public SimpleLayer() {
            params = getParameters();
            
            ScalarEncoder.Builder dayBuilder =
                ScalarEncoder.builder()
                    .n(8)
                    .w(3)
                    .radius(1.0)
                    .minVal(1.0)
                    .maxVal(8)
                    .periodic(true)
                    .forced(true)
                    .resolution(1);
            encoder = dayBuilder.build();
            
            spatialPooler = new SpatialPooler();
            
            temporalMemory = new TemporalMemory();
            
            Map<String, Object> anomalyParams = new HashMap<>();
            anomalyParams.put(KEY_MODE, Mode.PURE);
            anomaly = Anomaly.create(anomalyParams);
            
            configure();
        }
        
        public SimpleLayer(Parameters p, ScalarEncoder e, SpatialPooler s, TemporalMemory t, Anomaly a) {
            this.params = p;
            this.encoder = e;
            this.spatialPooler = s;
            this.temporalMemory = t;
            this.anomaly = a;
            
            configure();
        }
        
        private void configure() {
            columnCount = ((int[])params.get(KEY.COLUMN_DIMENSIONS))[0];
            params.apply(memory);
            spatialPooler.init(memory);
            TemporalMemory.init(memory);

            columnCount = memory.getPotentialPools().getMaxIndex() + 1; //If necessary, flatten multi-dimensional index
            cellsPerColumn = memory.getCellsPerColumn();
        }
        
        public void input(double value , int recordNum, int seqNum) {
            String recordOut = stringValue(value);
            
            if(doPrintout && value == 1) {
                System.out.println("--------------------------------------------------------");
                System.out.println("Iteration: " + recordNum);
            }
            if(doPrintout) System.out.println("===== " + recordOut + "  - Sequence Num: " + seqNum + " =====");
            
            //Input through encoder
            if(doPrintout) System.out.println("ScalarEncoder Input = " + value);
            int[] encoding = encoder.encode(value);
            if(doPrintout) System.out.println("ScalarEncoder Output = " + Arrays.toString(encoding));
            
            //Input through spatial pooler
            int[] output = new int[columnCount];
            spatialPooler.compute(memory, encoding, output, true);
            if(doPrintout) System.out.println("SpatialPooler Output = " + Arrays.toString(output));
            
            //Input through temporal memory
            int[] input = ArrayUtils.where(output, ArrayUtils.WHERE_1);
            ComputeCycle cc = temporalMemory.compute(memory, input, true);
            prevPredictedColumns = predictedColumns;
            predictedColumns = SDR.cellsAsColumnIndices(cc.predictiveCells(), cellsPerColumn); //Get the predicted column indexes
            if(doPrintout) System.out.println("TemporalMemory Input = " + Arrays.toString(input));
            if(doPrintout) System.out.println("TemporalMemory Prediction = " + Arrays.toString(predictedColumns));
            Set<Cell> actives = cc.activeCells();
            int[] actCellIndices = SDR.asCellIndices(actives);
            if(doPrintout) System.out.println("TemporalMemory Active Cells = " + Arrays.toString(actCellIndices));
            Set<Cell> pred = cc.predictiveCells();
            int[] predCellIndices = SDR.asCellIndices(pred);
            if(doPrintout) System.out.println("TemporalMemory Predictive Cells = " + Arrays.toString(predCellIndices));
            
            //Anomaly 
            double score = anomaly.compute(input, prevPredictedColumns, 0.0, 0);
            if(doPrintout) System.out.println("Anomaly Score = " + score);
            
            if(recordNum == SAMPLE_WEEK) {
                simpleSamples.add(new SampleWeek(seqNum, encoding, output, input, predictedColumns, actCellIndices, predCellIndices, score));
            }
        }
    }
    
    class SampleWeek {
        int seqNum;
        int[] encoderOut, spOut, tmIn, tmPred, activeCells, predictiveCells;
        double score;
        
        public SampleWeek(int seq, int[] enc, int[] spo, int[] tmin, int[] tmpred, int[] actCells, int[] predCells, double sc) {
            seqNum = seq;
            encoderOut = enc;
            spOut = spo;
            tmIn = tmin;
            tmPred = tmpred;
            activeCells = actCells;
            predictiveCells = predCells;
            score = sc;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Arrays.hashCode(encoderOut);
            long temp;
            temp = Double.doubleToLongBits(score);
            result = prime * result + (int)(temp ^ (temp >>> 32));
            result = prime * result + seqNum;
            result = prime * result + Arrays.hashCode(spOut);
            result = prime * result + Arrays.hashCode(tmIn);
            result = prime * result + Arrays.hashCode(tmPred);
            result = prime * result + Arrays.hashCode(activeCells);
            result = prime * result + Arrays.hashCode(predictiveCells);
            return result;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            SampleWeek other = (SampleWeek)obj;
            if(!Arrays.equals(encoderOut, other.encoderOut))
                return false;
            if(Double.doubleToLongBits(score) != Double.doubleToLongBits(other.score))
                return false;
            if(seqNum != other.seqNum)
                return false;
            if(!Arrays.equals(spOut, other.spOut))
                return false;
            if(!Arrays.equals(tmIn, other.tmIn))
                return false;
            if(!Arrays.equals(tmPred, other.tmPred))
                return false;
            if(!Arrays.equals(activeCells, other.activeCells))
                return false;
            if(!Arrays.equals(predictiveCells, other.predictiveCells))
                return false;
            return true;
        }

    }

}