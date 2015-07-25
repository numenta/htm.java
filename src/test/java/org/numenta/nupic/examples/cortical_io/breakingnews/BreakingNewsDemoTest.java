package org.numenta.nupic.examples.cortical_io.breakingnews;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.cortical.rest.model.Metric;
import io.cortical.twitter.Algorithm;
import io.cortical.twitter.Tweet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.network.Network;


public class BreakingNewsDemoTest {

    int count = 0;
    @Test
    public void testRunAlgorithm() {
        count = 0;
        
        List<String> strings = new ArrayList<>();
        strings.add("{'text': \"text 1\", 'created_at': 'Fri Feb 01 12:04:03  2013'}");
        strings.add("{'text': \"text 2\", 'created_at': 'Fri Feb 01 12:05:03  2013'}");
        Stream<String> stream = strings.stream();
        
        Algorithm algo = getAlgorithm(t -> {
            ++count;
            if(count == 1) {
                assertNotNull(t.getText());
                assertTrue(t.getText().equals("text 1"));
                assertNotNull(t.getDateTime());
                assertTrue(t.getDateTime().getHourOfDay() == 12);
            }else{
                assertNotNull(t.getText());
                assertTrue(t.getText().equals("text 2"));
                assertNotNull(t.getDateTime());
                assertTrue(t.getDateTime().getHourOfDay() == 12);
            }
        });
        
        BreakingNewsDemo demo = new BreakingNewsDemo();
        demo.runAlgorithm(algo, stream);
        
        assertNotNull(algo);
        assertEquals(2, count);
    }
    
    @Test
    public void testGetParameters() {
        BreakingNewsDemo demo = new BreakingNewsDemo();
        Parameters p = demo.getHTMParameters();
        assertNotNull(p);
        assertTrue(Arrays.equals(new int[] { 16384 }, (int[])p.getParameterByKey(KEY.COLUMN_DIMENSIONS)));
    }
    
    @Test
    public void testCreateDataStream() {
        BreakingNewsDemo demo = new BreakingNewsDemo();
        Stream<String> stream = demo.createDataStream();
        assertNotNull(stream);
    }
    
    private Algorithm getAlgorithm(Consumer<Tweet> c) {
        return new Algorithm() {

            @Override
            public void compute(Tweet arg0) {
                c.accept(arg0);
            }

            @Override public double getAnomaly() { return 0; }
            @Override public Tweet getCurrentTweet() { return null; }
            @Override public int[] getPrediction() { return null; }
            @Override public double getPrevAnomaly() { return 0; }
            @Override public int[] getPrevPrediction() { return null; }
            @Override public List<Tweet> getProcessedTweets() { return null; }
            @Override public Metric getSimilarities() { return null; }
            @Override public List<Tweet> getSimilarityHistory() { return null; }
            @Override public void listenToNetwork(Network arg0) {}
        };
    }
}
