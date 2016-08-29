package org.numenta.nupic;

import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;
import static org.numenta.nupic.algorithms.Anomaly.KEY_USE_MOVING_AVG;
import static org.numenta.nupic.algorithms.Anomaly.KEY_WINDOW_SIZE;

import java.io.IOException;
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
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;
import org.numenta.nupic.util.UniversalRandom;

import gnu.trove.list.array.TIntArrayList;

public class QuickTest {
    
    
    public static class Layer {
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
        
        private static String INPUT_PATH = "/Users/cogmission/git/NAB/data/artificialNoAnomaly/art_daily_no_noise.csv";
        private static String readFile = "/Users/cogmission/git/NAB/data/artificialNoAnomaly/art_daily_sp_output.txt";
        private static List<int[]> input;
        private static List<String> raw;
        
        @SuppressWarnings("unused")
        private static final int[] FIRST_LINE_CELLS = { 
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 
            25, 26, 27, 28, 29, 30, 31, 2784, 2785, 2786, 2787, 2788, 2789, 2790, 2791, 2792, 2793, 
            2794, 2795, 2796, 2797, 2798, 2799, 2800, 2801, 2802, 2803, 2804, 2805, 2806, 2807, 2808, 
            2809, 2810, 2811, 2812, 2813, 2814, 2815, 3072, 3073, 3074, 3075, 3076, 3077, 3078, 3079, 
            3080, 3081, 3082, 3083, 3084, 3085, 3086, 3087, 3088, 3089, 3090, 3091, 3092, 3093, 3094, 
            3095, 3096, 3097, 3098, 3099, 3100, 3101, 3102, 3103, 4096, 4097, 4098, 4099, 4100, 4101, 
            4102, 4103, 4104, 4105, 4106, 4107, 4108, 4109, 4110, 4111, 4112, 4113, 4114, 4115, 4116, 
            4117, 4118, 4119, 4120, 4121, 4122, 4123, 4124, 4125, 4126, 4127, 4640, 4641, 4642, 4643, 
            4644, 4645, 4646, 4647, 4648, 4649, 4650, 4651, 4652, 4653, 4654, 4655, 4656, 4657, 4658, 
            4659, 4660, 4661, 4662, 4663, 4664, 4665, 4666, 4667, 4668, 4669, 4670, 4671, 4832, 4833, 
            4834, 4835, 4836, 4837, 4838, 4839, 4840, 4841, 4842, 4843, 4844, 4845, 4846, 4847, 4848, 
            4849, 4850, 4851, 4852, 4853, 4854, 4855, 4856, 4857, 4858, 4859, 4860, 4861, 4862, 4863, 
            5216, 5217, 5218, 5219, 5220, 5221, 5222, 5223, 5224, 5225, 5226, 5227, 5228, 5229, 5230, 
            5231, 5232, 5233, 5234, 5235, 5236, 5237, 5238, 5239, 5240, 5241, 5242, 5243, 5244, 5245, 
            5246, 5247, 5760, 5761, 5762, 5763, 5764, 5765, 5766, 5767, 5768, 5769, 5770, 5771, 5772, 
            5773, 5774, 5775, 5776, 5777, 5778, 5779, 5780, 5781, 5782, 5783, 5784, 5785, 5786, 5787, 
            5788, 5789, 5790, 5791, 5856, 5857, 5858, 5859, 5860, 5861, 5862, 5863, 5864, 5865, 5866, 
            5867, 5868, 5869, 5870, 5871, 5872, 5873, 5874, 5875, 5876, 5877, 5878, 5879, 5880, 5881, 
            5882, 5883, 5884, 5885, 5886, 5887, 6976, 6977, 6978, 6979, 6980, 6981, 6982, 6983, 6984, 
            6985, 6986, 6987, 6988, 6989, 6990, 6991, 6992, 6993, 6994, 6995, 6996, 6997, 6998, 6999, 
            7000, 7001, 7002, 7003, 7004, 7005, 7006, 7007, 7456, 7457, 7458, 7459, 7460, 7461, 7462, 
            7463, 7464, 7465, 7466, 7467, 7468, 7469, 7470, 7471, 7472, 7473, 7474, 7475, 7476, 7477, 
            7478, 7479, 7480, 7481, 7482, 7483, 7484, 7485, 7486, 7487, 7744, 7745, 7746, 7747, 7748, 
            7749, 7750, 7751, 7752, 7753, 7754, 7755, 7756, 7757, 7758, 7759, 7760, 7761, 7762, 7763, 
            7764, 7765, 7766, 7767, 7768, 7769, 7770, 7771, 7772, 7773, 7774, 7775, 8000, 8001, 8002, 
            8003, 8004, 8005, 8006, 8007, 8008, 8009, 8010, 8011, 8012, 8013, 8014, 8015, 8016, 8017, 
            8018, 8019, 8020, 8021, 8022, 8023, 8024, 8025, 8026, 8027, 8028, 8029, 8030, 8031, 8320, 
            8321, 8322, 8323, 8324, 8325, 8326, 8327, 8328, 8329, 8330, 8331, 8332, 8333, 8334, 8335, 
            8336, 8337, 8338, 8339, 8340, 8341, 8342, 8343, 8344, 8345, 8346, 8347, 8348, 8349, 8350, 
            8351, 8448, 8449, 8450, 8451, 8452, 8453, 8454, 8455, 8456, 8457, 8458, 8459, 8460, 8461, 
            8462, 8463, 8464, 8465, 8466, 8467, 8468, 8469, 8470, 8471, 8472, 8473, 8474, 8475, 8476, 
            8477, 8478, 8479, 9248, 9249, 9250, 9251, 9252, 9253, 9254, 9255, 9256, 9257, 9258, 9259, 
            9260, 9261, 9262, 9263, 9264, 9265, 9266, 9267, 9268, 9269, 9270, 9271, 9272, 9273, 9274, 
            9275, 9276, 9277, 9278, 9279, 9280, 9281, 9282, 9283, 9284, 9285, 9286, 9287, 9288, 9289, 
            9290, 9291, 9292, 9293, 9294, 9295, 9296, 9297, 9298, 9299, 9300, 9301, 9302, 9303, 9304, 
            9305, 9306, 9307, 9308, 9309, 9310, 9311, 9696, 9697, 9698, 9699, 9700, 9701, 9702, 9703, 
            9704, 9705, 9706, 9707, 9708, 9709, 9710, 9711, 9712, 9713, 9714, 9715, 9716, 9717, 9718, 
            9719, 9720, 9721, 9722, 9723, 9724, 9725, 9726, 9727, 9984, 9985, 9986, 9987, 9988, 9989, 
            9990, 9991, 9992, 9993, 9994, 9995, 9996, 9997, 9998, 9999, 10000, 10001, 10002, 10003, 
            10004, 10005, 10006, 10007, 10008, 10009, 10010, 10011, 10012, 10013, 10014, 10015, 10016, 
            10017, 10018, 10019, 10020, 10021, 10022, 10023, 10024, 10025, 10026, 10027, 10028, 10029, 
            10030, 10031, 10032, 10033, 10034, 10035, 10036, 10037, 10038, 10039, 10040, 10041, 10042, 
            10043, 10044, 10045, 10046, 10047, 10688, 10689, 10690, 10691, 10692, 10693, 10694, 10695, 
            10696, 10697, 10698, 10699, 10700, 10701, 10702, 10703, 10704, 10705, 10706, 10707, 10708, 
            10709, 10710, 10711, 10712, 10713, 10714, 10715, 10716, 10717, 10718, 10719, 10720, 10721, 
            10722, 10723, 10724, 10725, 10726, 10727, 10728, 10729, 10730, 10731, 10732, 10733, 10734, 
            10735, 10736, 10737, 10738, 10739, 10740, 10741, 10742, 10743, 10744, 10745, 10746, 10747, 
            10748, 10749, 10750, 10751, 10784, 10785, 10786, 10787, 10788, 10789, 10790, 10791, 10792, 
            10793, 10794, 10795, 10796, 10797, 10798, 10799, 10800, 10801, 10802, 10803, 10804, 10805, 
            10806, 10807, 10808, 10809, 10810, 10811, 10812, 10813, 10814, 10815, 10944, 10945, 10946, 
            10947, 10948, 10949, 10950, 10951, 10952, 10953, 10954, 10955, 10956, 10957, 10958, 10959, 
            10960, 10961, 10962, 10963, 10964, 10965, 10966, 10967, 10968, 10969, 10970, 10971, 10972, 
            10973, 10974, 10975, 11072, 11073, 11074, 11075, 11076, 11077, 11078, 11079, 11080, 11081, 
            11082, 11083, 11084, 11085, 11086, 11087, 11088, 11089, 11090, 11091, 11092, 11093, 11094, 
            11095, 11096, 11097, 11098, 11099, 11100, 11101, 11102, 11103, 11104, 11105, 11106, 11107, 
            11108, 11109, 11110, 11111, 11112, 11113, 11114, 11115, 11116, 11117, 11118, 11119, 11120, 
            11121, 11122, 11123, 11124, 11125, 11126, 11127, 11128, 11129, 11130, 11131, 11132, 11133, 
            11134, 11135, 11296, 11297, 11298, 11299, 11300, 11301, 11302, 11303, 11304, 11305, 11306, 
            11307, 11308, 11309, 11310, 11311, 11312, 11313, 11314, 11315, 11316, 11317, 11318, 11319, 
            11320, 11321, 11322, 11323, 11324, 11325, 11326, 11327, 11360, 11361, 11362, 11363, 11364, 
            11365, 11366, 11367, 11368, 11369, 11370, 11371, 11372, 11373, 11374, 11375, 11376, 11377, 
            11378, 11379, 11380, 11381, 11382, 11383, 11384, 11385, 11386, 11387, 11388, 11389, 11390, 
            11391, 11392, 11393, 11394, 11395, 11396, 11397, 11398, 11399, 11400, 11401, 11402, 11403, 
            11404, 11405, 11406, 11407, 11408, 11409, 11410, 11411, 11412, 11413, 11414, 11415, 11416, 
            11417, 11418, 11419, 11420, 11421, 11422, 11423, 11424, 11425, 11426, 11427, 11428, 11429, 
            11430, 11431, 11432, 11433, 11434, 11435, 11436, 11437, 11438, 11439, 11440, 11441, 11442, 
            11443, 11444, 11445, 11446, 11447, 11448, 11449, 11450, 11451, 11452, 11453, 11454, 11455, 
            11680, 11681, 11682, 11683, 11684, 11685, 11686, 11687, 11688, 11689, 11690, 11691, 11692, 
            11693, 11694, 11695, 11696, 11697, 11698, 11699, 11700, 11701, 11702, 11703, 11704, 11705, 
            11706, 11707, 11708, 11709, 11710, 11711, 11840, 11841, 11842, 11843, 11844, 11845, 11846, 
            11847, 11848, 11849, 11850, 11851, 11852, 11853, 11854, 11855, 11856, 11857, 11858, 11859, 
            11860, 11861, 11862, 11863, 11864, 11865, 11866, 11867, 11868, 11869, 11870, 11871, 12032, 
            12033, 12034, 12035, 12036, 12037, 12038, 12039, 12040, 12041, 12042, 12043, 12044, 12045, 
            12046, 12047, 12048, 12049, 12050, 12051, 12052, 12053, 12054, 12055, 12056, 12057, 12058, 
            12059, 12060, 12061, 12062, 12063, 12288, 12289, 12290, 12291, 12292, 12293, 12294, 12295, 
            12296, 12297, 12298, 12299, 12300, 12301, 12302, 12303, 12304, 12305, 12306, 12307, 12308, 
            12309, 12310, 12311, 12312, 12313, 12314, 12315, 12316, 12317, 12318, 12319, 12320, 12321, 
            12322, 12323, 12324, 12325, 12326, 12327, 12328, 12329, 12330, 12331, 12332, 12333, 12334, 
            12335, 12336, 12337, 12338, 12339, 12340, 12341, 12342, 12343, 12344, 12345, 12346, 12347, 
            12348, 12349, 12350, 12351, 12512, 12513, 12514, 12515, 12516, 12517, 12518, 12519, 12520, 
            12521, 12522, 12523, 12524, 12525, 12526, 12527, 12528, 12529, 12530, 12531, 12532, 12533, 
            12534, 12535, 12536, 12537, 12538, 12539, 12540, 12541, 12542, 12543, 12544, 12545, 12546, 
            12547, 12548, 12549, 12550, 12551, 12552, 12553, 12554, 12555, 12556, 12557, 12558, 12559, 
            12560, 12561, 12562, 12563, 12564, 12565, 12566, 12567, 12568, 12569, 12570, 12571, 12572, 
            12573, 12574, 12575, 12576, 12577, 12578, 12579, 12580, 12581, 12582, 12583, 12584, 12585, 
            12586, 12587, 12588, 12589, 12590, 12591, 12592, 12593, 12594, 12595, 12596, 12597, 12598, 
            12599, 12600, 12601, 12602, 12603, 12604, 12605, 12606, 12607, 13056, 13057, 13058, 13059, 
            13060, 13061, 13062, 13063, 13064, 13065, 13066, 13067, 13068, 13069, 13070, 13071, 13072, 
            13073, 13074, 13075, 13076, 13077, 13078, 13079, 13080, 13081, 13082, 13083, 13084, 13085, 
            13086, 13087, 15392, 15393, 15394, 15395, 15396, 15397, 15398, 15399, 15400, 15401, 15402, 
            15403, 15404, 15405, 15406, 15407, 15408, 15409, 15410, 15411, 15412, 15413, 15414, 15415, 
            15416, 15417, 15418, 15419, 15420, 15421, 15422, 15423 };

        
        
        /**
         * Makeshift Layer to contain and operate on algorithmic entities
         * 
         * @param c         the {@link Connections} object.
         * @param encoder   the {@link MultiEncoder}
         * @param sp        the {@link SpatialPooler}
         * @param tm        the {@link TemporalMemory}
         * @param cl        the {@link CLAClassifier}
         */
        public Layer(Connections c, MultiEncoder encoder, SpatialPooler sp, 
            TemporalMemory tm, CLAClassifier cl, Anomaly anomaly) {
            
            this.connections = c;
            this.encoder = encoder;
            this.sp = sp;
            this.tm = tm;
            this.classifier = cl;
        }
        
        public void printHeader() {
            System.out.println("--------------------------------------------");
            System.out.println("Record #: " + recordNum + "\n");
            System.out.println("Raw Input: " + Layer.raw.get(recordNum + 1));
        }
        
        /**
         * Input through Encoder
         * 
         * @param timestamp
         * @param value
         * @param isVerbose
         * @return Tuple { encoding, bucket index }
         */
        public Tuple encodingStep(DateTime timestamp, double value, boolean isVerbose) {
            Map<String, Object> encodingInput = new HashMap<String, Object>();
            encodingInput.put("value", value);
            encodingInput.put("timestamp",  timestamp);
            int[] encoding = encoder.encode(encodingInput);
            if(isVerbose) {
                System.out.println("ScalarEncoder Output = " + Arrays.toString(encoding));
            }
            
            int bucketIdx = encoder.getBucketIndices(value)[0];
            
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
            sp.compute(connections, encoding, output, true, true);
            int[] sparseSPOutput = ArrayUtils.where(output, ArrayUtils.WHERE_1);
            if(isVerbose) {
                System.out.println("SpatialPooler Output = " + Arrays.toString(output));
            }
            
            return new Tuple(output, sparseSPOutput);
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
            int[] predColumnIndices = SDR.cellsAsColumnIndices(cc.predictiveCells(), connections.cellsPerColumn);
            int[] activeColumns = Arrays.stream(activeCellIndices)
                .map(cell -> cell / connections.cellsPerColumn)
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
    
    public static Layer createLayer() {
        UniversalRandom random = new UniversalRandom(42);
        
//        MultiEncoder encoder = createEncoder();
        
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
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 1000);
        parameters.set(KEY.MAX_BOOST, 2.0);
        //parameters.setParameterByKey(KEY.SEED, 42);
        parameters.set(KEY.SP_VERBOSITY, 0);

        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.set(KEY.MIN_THRESHOLD, 5);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.1);//0.05
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.1);//0.05
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);
        
        //////////////////////////////////////////////////////////
//        testRandom(random);
        //////////////////////////////////////////////////////////
        
        //////////////////////////////////////////////////////////
//        int[] encoding = testEncoder(encoder);
        //////////////////////////////////////////////////////////
        
        Connections conn = new Connections();
        parameters.apply(conn);
        
//        SpatialPooler sp = new SpatialPooler();
//        sp.init(conn);
        
        //////////////////////////////////////////////////////////
//        int[] sparseSdr = testSpatialPooler(sp, conn, encoding);
        //////////////////////////////////////////////////////////
        
        TemporalMemory tm = new TemporalMemory();
        TemporalMemory.init(conn);
        
        //////////////////////////////////////////////////////////
        // Tuple = (activeCells, predictiveCells)
//        testTemporalMemory(tm, conn, sparseSdr);
        //////////////////////////////////////////////////////////
        
        CLAClassifier cl = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.3, 0);
        
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
        
        return new Layer(conn, null, null, tm, cl, anomalyComputer);
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
        /*
        String j = "[0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
           "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, " +
           "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]";
        */
        return output;
    }
    
    public static int[] testSpatialPooler(SpatialPooler sp, Connections conn, int[] encoding) {
        int[] dense = new int[2048];
        sp.compute(conn, encoding, dense, true, true);
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
        try (Stream<String> stream = Files.lines(Paths.get(Layer.readFile))) {
            Layer.input = stream.map(l -> {
                String line = l.replace("[", "").replace("]",  "").trim();
                int[] result = Arrays.stream(line.split("[\\s]*\\,[\\s]*")).mapToInt(i -> Integer.parseInt(i)).toArray();
                return result;
            }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void loadRawInputFile() {
        try (Stream<String> stream = Files.lines(Paths.get(Layer.INPUT_PATH))) {
            Layer.raw = stream.map(l -> l.trim()).collect(Collectors.toList());
        }catch(Exception e) {e.printStackTrace();}
    }
    
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        boolean IS_VERBOSE = true;
        boolean LEARN = true;
        
        QuickTest.Layer layer = QuickTest.createLayer();
        
        System.out.println("\n===================================\n");
        
        loadSPOutputFile();
        loadRawInputFile();
        
        if(Layer.input != null) {
            for(int i = 0;i < Layer.input.size();i++) {
                layer.printHeader();
                int[] sparseSPOutput = Layer.input.get(i);
                Tuple tmTuple = layer.tmStep(sparseSPOutput, LEARN, IS_VERBOSE);
                double score = layer.anomalyStep(sparseSPOutput, (int[])tmTuple.get(1), true);
                layer.incRecordNum();
                
                // Store the current prediction as previous
                layer.storeCyclePrediction((int[])tmTuple.get(2));
            }
        }else{
            DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            try (Stream<String> stream = Files.lines(Paths.get(Layer.INPUT_PATH))) {
                stream.forEach(l -> {
                    layer.printHeader();
                    String[] line = l.split("[\\s]*\\,[\\s]*");
                    DateTime timestamp = formatter.parseDateTime(line[0].trim());
                    double value = Double.parseDouble(line[1].trim());
                    Tuple encTuple = layer.encodingStep(timestamp, value, IS_VERBOSE);
                    Tuple spTuple = layer.spStep((int[])encTuple.get(0), LEARN, IS_VERBOSE);
                    Tuple tmTuple = layer.tmStep((int[])spTuple.get(1), LEARN, IS_VERBOSE); 
                    Classification<Double> cla = layer.classificationStep((int[])tmTuple.get(0), (int)encTuple.get(1), value, LEARN, IS_VERBOSE);
                    double score = layer.anomalyStep((int[])spTuple.get(1), layer.prevPredictedCols, IS_VERBOSE);
                
                    layer.incRecordNum();
                    
                    // Store the current prediction as previous
                    layer.storeCyclePrediction((int[])tmTuple.get(2));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
