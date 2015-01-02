package org.numenta.nupic.encoders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.encoders.*;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

import static org.junit.Assert.*;

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
        
        bits = de.encode(dt.toDate());

        //
        //dt.getMillis();

        // season is aaabbbcccddd (1 bit/month) # TODO should be <<3?
        // should be 000000000111 (centered on month 11 - Nov)
        int[] seasonExpected = {0,0,0,0,0,0,0,0,0,1,1,1};

        // week is MTWTFSS
        // contrary to localtime documentation, Monday = 0 (for python
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

        List<Tuple> expectedDescs = new ArrayList<Tuple>(Arrays.asList(
                new Tuple(2, "season", 0),
                new Tuple(2, "day of week", 12),
                new Tuple(2, "weekend", 19),
                new Tuple(2, "time of day", 25)
        ));

        assertEquals(expectedDescs.size(), descs.size());

        for (int i = 0; i < expectedDescs.size(); ++i) {
            Tuple desc = descs.get(i);
            assertNotNull(desc);
            assertEquals(expectedDescs.get(i), desc);
        }

        assertTrue(Arrays.equals(expected, bits));

        System.out.println();
        de.pprintHeader("");
        de.pprint(bits, "");
        System.out.println();
    }
}

