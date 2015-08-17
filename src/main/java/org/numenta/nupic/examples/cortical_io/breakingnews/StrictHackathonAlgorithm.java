package org.numenta.nupic.examples.cortical_io.breakingnews;

import gnu.trove.list.array.TIntArrayList;
import io.cortical.rest.model.Fingerprint;
import io.cortical.rest.model.Metric;
import io.cortical.services.RetinaApis;
import io.cortical.services.api.client.ApiException;
import io.cortical.twitter.Algorithm;
import io.cortical.twitter.Tweet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;

import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.util.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * An implementation of {@link Algorithm} that closely follows the original
 * logic used for the hackathon hack (as opposed to other experimental variations
 * this author may propose).
 * 
 * @author cogmission
 * @see Algorithm
 */
public class StrictHackathonAlgorithm implements Algorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(StrictHackathonAlgorithm.class);
    
    private static final int SDR_WIDTH = 16384;
    private static final double SPARSITY = 0.02;
    
    private RetinaApis retinaApis;
    private Network htmNetwork;
    
    private Metric similarities;
    
    private int[] currentPrediction;
    private int[] prevPrediction;
    
    private double anomaly;
    private double prevAnomaly;
    
    private List<Tweet> processedTweets;
    private List<Tweet> anomalousTweets;
    private List<Tweet> highSimilarityTweets;
    
    private Tweet currentTweet;
    
    
    
    /** Ensures synchronous processing between input and listener */
    private Semaphore semaphore;
    
    private Random rng;
    
    /**
     * Constructs a new {@code StrictHackathonAlgorithm}
     * 
     * @param retinaApis
     * @param htmNetwork
     */
    public StrictHackathonAlgorithm(RetinaApis retinaApis, Network htmNetwork) {
        this.retinaApis = retinaApis;
        this.htmNetwork = htmNetwork;
        
        processedTweets = new ArrayList<>();
        anomalousTweets = new ArrayList<>();
        highSimilarityTweets = new ArrayList<>();
        
        semaphore = new Semaphore(1, false);
        
        rng = new Random();
        
        if(htmNetwork != null) {
            listenToNetwork(htmNetwork);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void listenToNetwork(Network network) {
        if(network == null) {
            throw new IllegalArgumentException("Cannot listen to null Network.");
        }
        
        this.htmNetwork = network;
                        
        network.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) {}
            @Override public void onNext(Inference t) {
                currentPrediction = subsample(t.getSDR());
                semaphore.release();
            }
        });
    }
    
    /**
     * Returns the current tweet that the most recent call to {@link #compute(Tweet)}
     * was called with.
     * 
     * @return
     */
    @Override
    public Tweet getCurrentTweet() {
        return currentTweet;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void compute(Tweet tweet) {
        currentTweet = tweet;
        
        List<Fingerprint> tweetFp = Collections.emptyList();
        
        try {
            tweetFp = retinaApis.textApi().getFingerprints(tweet.getText());
            tweet.setFingerprints(tweetFp);
            processedTweets.add(tweet);
        } catch(ApiException e) {
            System.out.println("failed tweet: " + tweet.getText() + "\nfailed json: " + tweet.getJson());
            e.printStackTrace();
            return;
        }
        
        if(currentPrediction != null && currentPrediction.length > 0 && !tweetFp.isEmpty() &&
            currentPrediction.length > prevPrediction.length / 2 ) {
            
            similarities = compare(tweetFp.get(0).getPositions(), currentPrediction);
            anomaly = 1.0 - Math.max(similarities.getOverlappingLeftRight(), similarities.getOverlappingRightLeft());             
        }else{
            anomaly = 1.0;
            similarities = null;
        }
        
        prevPrediction = currentPrediction;
        
        if(anomaly > prevAnomaly + 0.2) {
            anomalousTweets.add(tweet);
        }else if(anomaly < prevAnomaly - 0.2) {
            inspectTweetHistory(
                tweet,
                processedTweets.subList(Math.max(0, processedTweets.size() - 20),  processedTweets.size()));
        }
        
        prevAnomaly = anomaly;
        tweet.setAnomaly(anomaly);
        
        if(tweetFp.get(0).getPositions().length > 300) {
            try {
                semaphore.acquire();
            } catch(InterruptedException e) {
                LOGGER.error("Could not acquire lock to proceed with processing next tweet", e);
            }
            
            htmNetwork.compute(tweetFp.get(0).getPositions());
        }
    }
    
    /**
     * Returns the percent sparsity of the specified SDR.
     * 
     * @param sdr
     * @return
     */
    double getSparsity(int[] sdr) {
        double sparsity = (double)sdr.length / (double)SDR_WIDTH;
        return sparsity;
    }
    
    /**
     * Returns the randomly culled array whose entries are equal
     * to {@link #SPARSITY}
     * 
     * @param input     the int array to ensure proper sparsity for.
     * @return
     */
    int[] subsample(int[] input) {
        double sparsity = getSparsity(input);
        if(sparsity > 0.02) {
           input = ArrayUtils.sample((int)(SDR_WIDTH * SPARSITY) + 1, new TIntArrayList(input), rng); 
        }
        return input;
    }
    
    void inspectTweetHistory(Tweet tweet, List<Tweet> pastTweets) {
        // Make sure we only retain history for current call
        highSimilarityTweets.clear();
        
        for(Tweet pastTweet: pastTweets) {
            Metric sims = compare(
                tweet.getFingerprints().get(0).getPositions(), 
                    pastTweet.getFingerprints().get(0).getPositions());
            
            double anomaly = 1.0 - Math.max(sims.getOverlappingLeftRight(), sims.getOverlappingRightLeft());
            
            if(anomaly < 0.50) {
                pastTweet.setAnomaly(anomaly);
                highSimilarityTweets.add(pastTweet);
            }
        }
    }
    
    Metric compare(int[] tweetPositions, int[] htmPrediction) {
        String jsonTP = "{ \"positions\" : " + Arrays.toString(tweetPositions) + " }";
        String jsonHTM = "{ \"positions\" : " + Arrays.toString(htmPrediction) + " }";
        try {
            return retinaApis.compareApi().compare(jsonTP, jsonHTM);
        }catch(ApiException e) {
            LOGGER.error("Could not retreive comparison for last tweet and prediction.");
        } catch(JsonProcessingException e) {
            LOGGER.error("Problem with json from last comparison:\n1. " + jsonTP + "\n2. " + jsonHTM);
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public double getAnomaly() {
        return anomaly;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public double getPrevAnomaly() {
        return prevAnomaly;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getPrediction() {
        return currentPrediction;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getPrevPrediction() {
        return prevPrediction;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Metric getSimilarities() {
        return similarities;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tweet> getSimilarityHistory() {
        return highSimilarityTweets;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Tweet> getProcessedTweets() {
        return processedTweets;
    }

}
