package org.numenta.nupic.unit.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.encoders.*;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;

import static org.junit.Assert.*;

public class DateEncoderTest {
    private DateEncoder de;
    private Parameters parameters;
    private DateTime dt;
    private int[] expected;
    private int[] bits;

    private void setUp() {
        // 3 bits for season, 1 bit for day of week, 3 for weekend, 5 for time of day
        // use of forced is not recommended, used here for readability.
        parameters = new Parameters();
        EnumMap<Parameters.KEY, Object> p = parameters.getMap();
        p.put(KEY.SEASON, 3);
        p.put(KEY.DAY_OF_WEEK, 1);
        p.put(KEY.WEEKEND, 3);
        p.put(KEY.TIME_OF_DAY, 5);


        //in the middle of fall, Thursday, not a weekend, afternoon - 4th Nov, 2010, 14:55
        dt = new DateTime(2010, 11, 4, 14, 55);

        //FIXME how to pass the datetime in, it should not be string
        bits = de.encode(dt.toString("yyyy-MM-dd HH:mm:ss"));

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
        de = new DateEncoder();
        Parameters.apply(de, parameters);
        de.init();
    }

    /**
     * Creating date encoder instance
     */
    @Test
    public void testDateEncoder() {
        setUp();
        initDE();

        int[] empty = de.encode(Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
        System.out.println("\nEncoded missing data as: " + Arrays.toString(empty));
        int[] expected = new int[14];
        assertTrue(Arrays.equals(expected, empty));

        List<Tuple> desc = de.getDescription();
        assertNotNull(desc);
        // should be [("season", 0), ("day of week", 12), ("weekend", 19), ("time of day", 25)]

        Tuple descSeason = desc.get(0);
        assertNotNull(descSeason);
        assertEquals("season", descSeason.get(0));
        assertEquals(0, descSeason.get(1));

        Tuple descDayOfWeek = desc.get(0);
        assertNotNull(descDayOfWeek);
        assertEquals("day of week", descDayOfWeek.get(0));
        assertEquals(12, descDayOfWeek.get(1));

        Tuple descWeekend = desc.get(0);
        assertNotNull(descWeekend);
        assertEquals("weekend", descWeekend.get(0));
        assertEquals(19, descWeekend.get(1));

        Tuple descTimeOfDay = desc.get(0);
        assertNotNull(descTimeOfDay);
        assertEquals("time of day", descTimeOfDay.get(0));
        assertEquals(25, descWeekend.get(1));

        assertTrue(Arrays.equals(expected, bits));

        System.out.println();
        de.pprintHeader("");
        de.pprint(bits, "");
        System.out.println();
    }
}

