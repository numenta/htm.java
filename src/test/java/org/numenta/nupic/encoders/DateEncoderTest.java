package org.numenta.nupic.encoders;

import java.util.*;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class DateEncoderTest {
    private DateEncoder de;
    private DateEncoder.Builder builder;
    private DateTime dt;
    private int[] expected;
    private int[] bits;

    private void setUp() {
        // 3 bits for season, 1 bit for day of week, 3 for weekend, 5 for time of day
        // use of forced is not recommended, used here for readability.
        builder = DateEncoder.builder();

        de = builder.season(3)
                .dayOfWeek(1)
                .weekend(3)
                .timeOfDay(5).build();
        
        //in the middle of fall, Thursday, not a weekend, afternoon - 4th Nov, 2010, 14:55
        dt = new DateTime(2010, 11, 4, 14, 55);
        DateTime comparison = new DateTime(2010, 11, 4, 13, 55);
        
        bits = de.encode(dt);
        int[] comparisonBits = de.encode(comparison);
        
        System.out.println(Arrays.toString(bits));
        System.out.println(Arrays.toString(comparisonBits));

        //
        //dt.getMillis();

        // season is aaabbbcccddd (1 bit/month) # TODO should be <<3?
        // should be 000000000111 (centered on month 11 - Nov)
        int[] seasonExpected = {0,0,0,0,0,0,0,0,0,1,1,1};

        // week is MTWTFSS
        // contrary to local time documentation, Monday = 0 (for python
        //  datetime.datetime.timetuple()
        int[] dayOfWeekExpected = {0,0,0,1,0,0,0};

        // not a weekend, so it should be "False"
        int[] weekendExpected = {1,1,1,0,0,0};

        // time of day has radius of 4 hours and w of 5 so each bit = 240/5 min = 48min
        // 14:55 is minute 14*60 + 55 = 895; 895/48 = bit 18.6
        // should be 30 bits total (30 * 48 minutes = 24 hours)
        int[] timeOfDayExpected = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,1,1,1,1,0,0,0,0,0,0,0,0,0};

        expected = ArrayUtils.concatAll(seasonExpected, dayOfWeekExpected, weekendExpected, timeOfDayExpected);
    }

    private void initDE() {
        de = builder.build();
    }

    /**
     * Creating date encoder instance
     */
    @Test
    public void testDateEncoder() {
        setUp();
        initDE();

        List<Tuple> descs = de.getDescription();
        assertNotNull(descs);
        // should be [("season", 0), ("day of week", 12), ("weekend", 19), ("time of day", 25)]

        List<Tuple> expectedDescs = new ArrayList<>(Arrays.asList(
                new Tuple("season", 0),
                new Tuple("day of week", 12),
                new Tuple("weekend", 19),
                new Tuple("time of day", 25)
        ));

        assertEquals(expectedDescs.size(), descs.size());

        for (int i = 0; i < expectedDescs.size(); ++i) {
            Tuple desc = descs.get(i);
            assertNotNull(desc);
            assertEquals(expectedDescs.get(i), desc);
        }

        assertArrayEquals(expected, bits);

        System.out.println();
        de.pprintHeader("");
        de.pprint(bits, "");
        System.out.println();
    }

    // TODO Current implementation of DateEncoder throws at invalid Date,
    // but testMissingValues in Python expects it to encode it as all-zero bits:
    //    def testMissingValues(self):
    //            '''missing values'''
    //    mvOutput = self._e.encode(SENTINEL_VALUE_FOR_MISSING_DATA)
    //            self.assertEqual(sum(mvOutput), 0)

    /**
     * Decoding date
     */
    @Test
    public void testDecoding() {
        setUp();
        initDE();

        //TODO Why null is needed?
        Tuple decoded = de.decode(bits, null);

        Map<String, RangeList> fieldsMap = (Map<String, RangeList>)decoded.get(0);
        List<String> fieldsOrder = (List<String>)decoded.get(1);

        assertNotNull(fieldsMap);
        assertNotNull(fieldsOrder);
        assertEquals(4, fieldsMap.size());

        Map<String, Double> expectedMap = new HashMap<>();
        expectedMap.put("season", 305.0);
        expectedMap.put("time of day", 14.4);
        expectedMap.put("day of week", 3.0);
        expectedMap.put("weekend", 0.0);

        for (String key : expectedMap.keySet()) {
            double expected = expectedMap.get(key);
            RangeList actual = fieldsMap.get(key);
            assertEquals(1, actual.size());
            MinMax minmax = actual.getRange(0);
            assertEquals(expected, minmax.min(), de.getResolution());
            assertEquals(expected, minmax.max(), de.getResolution());
        }

        System.out.println(decoded.toString());
        System.out.println(String.format("decodedToStr=>%s", de.decodedToStr(decoded)));
    }

    /**
     * Check topDownCompute
     */
    @Test
    public void testTopDownCompute() {
        setUp();
        initDE();

        List<Encoding> topDown = de.topDownCompute(bits);

        List<Double> expectedList = Arrays.asList(320.25, 3.5, .167, 14.8);

        for (int i = 0; i < topDown.size(); i++) {
            Encoding r = topDown.get(i);
            double actual = (double)r.getValue();
            double expected = expectedList.get(i);
            assertEquals(expected, actual, 4.0);
        }
    }

    /**
     * Check bucket index support
     */
    @Test
    public void testBucketIndexSupport() {

        setUp();
        initDE();

        int[] bucketIndices = de.getBucketIndices(dt);
        System.out.println(String.format("bucket indices: %s", Arrays.toString(bucketIndices)));
        List<Encoding> bucketInfo = de.getBucketInfo(bucketIndices);

        List<Double> expectedList = Arrays.asList(320.25, 3.5, .167, 14.8);

        TIntList encodings = new TIntArrayList();

        for (int i = 0; i < bucketInfo.size(); i++) {
            Encoding r = bucketInfo.get(i);
            double actual = (double)r.getValue();
            double expected = expectedList.get(i);
            assertEquals(expected, actual, 4.0);

            encodings.addAll(r.getEncoding());
        }

        assertArrayEquals(expected, encodings.toArray());
    }

    /**
     * look at holiday more carefully because of the smooth transition
     */
    @Test
    public void testHoliday() {
        //use of forced is not recommended, used here for readability, see ScalarEncoder
        DateEncoder e = DateEncoder.builder().holiday(5).forced(true).build();
        int [] holiday = new int[]{0,0,0,0,0,1,1,1,1,1};
        int [] notholiday = new int[]{1,1,1,1,1,0,0,0,0,0};
        int [] holiday2 = new int[]{0,0,0,1,1,1,1,1,0,0};

        DateTime d = new DateTime(2010, 12, 25, 4, 55);
        //System.out.println(String.format("1:%s", Arrays.toString(e.encode(d))));
        assertArrayEquals(holiday, e.encode(d));

        d = new DateTime(2008, 12, 27, 4, 55);
        //System.out.println(String.format("2:%s", Arrays.toString(e.encode(d))));
        assertArrayEquals(notholiday, e.encode(d));

        d = new DateTime(1999, 12, 26, 8, 0);
        //System.out.println(String.format("3:%s", Arrays.toString(e.encode(d))));
        assertArrayEquals(holiday2, e.encode(d));

        d = new DateTime(2011, 12, 24, 16, 0);
        //System.out.println(String.format("4:%s", Arrays.toString(e.encode(d))));
        assertArrayEquals(holiday2, e.encode(d));
    }

    /**
     * Test weekend encoder
     */
    @Test
    public void testWeekend() {
        //use of forced is not recommended, used here for readability, see ScalarEncoder
        DateEncoder e = DateEncoder.builder().customDays(21, Arrays.asList(
                "sat", "sun", "fri"
        )).forced(true).build();
        DateEncoder mon = DateEncoder.builder().customDays(21, Arrays.asList(
                "Monday"
        )).forced(true).build();
        DateEncoder e2 = DateEncoder.builder().weekend(21, 1).forced(true).build();
        //DateTime d = new DateTime(1988,5,29,20,0);
        DateTime d = new DateTime(1988,5,29,20,0);

        assertArrayEquals(e.encode(d), e2.encode(d));

        for (int i = 0; i < 300; i++) {
            DateTime curDate = d.plusDays(i + 1);
            assertArrayEquals(e.encode(curDate), e2.encode(curDate));

            //Make sure
            Tuple decoded = mon.decode(mon.encode(curDate), null);

            Map<String, RangeList> fieldsMap = (Map<String, RangeList>)decoded.get(0);
            List<String> fieldsOrder = (List<String>)decoded.get(1);

            assertNotNull(fieldsMap);
            assertNotNull(fieldsOrder);
            assertEquals(1, fieldsMap.size());

            RangeList range = fieldsMap.get("Monday");
            assertEquals(1, range.size());
            assertEquals(1, ((List<MinMax>)range.get(0)).size());
            MinMax minmax = range.getRange(0);
            System.out.println("DateEncoderTest.testWeekend(): minmax.min() = " + minmax.min());

            if(minmax.min() == 1.0) {
                assertEquals(1, curDate.getDayOfWeek());
            } else {
                assertNotEquals(1, curDate.getDayOfWeek());
            }
        }
    }
}

