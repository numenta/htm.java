package org.numenta.nupic.algorithms;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.IOException;
import java.io.StringWriter;

import org.numenta.nupic.algorithms.AnomalyLikelihood.AnomalyParams;
import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;


public class AnomalyLikelihoodMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(AnomalyLikelihoodMetrics.class);
    
    private State state;
    
    private AnomalyParams params;
    private AveragedAnomalyRecordList aggRecordList;
    private double[] likelihoods;
    
    public AnomalyLikelihoodMetrics( double[] likelihoods, AveragedAnomalyRecordList aggRecordList, AnomalyParams params) {
        state = new State();
        
        this.params = params;
        this.aggRecordList = aggRecordList;
        this.likelihoods = likelihoods;
    }
    
    public double[] getLikelihoods() {
        return likelihoods;
    }
    
    public AveragedAnomalyRecordList getAvgRecordList() {
        return aggRecordList;
    }
    
    public AnomalyParams getParams() {
        return params;
    }
    
    public void updateState(Statistic s, Tuple movingAverage, TDoubleList historicalLikelihoods) {
        this.state = new State(s, movingAverage, historicalLikelihoods);
    }
    
    public String printState() {
        return state.toJson();
    }
    
    class State {
        private Statistic distribution;
        /** To contain TDoubleList:historicalValues, double:total, int:windowSize */
        private Tuple movingAverage;
        /** List of computed likelihoods of an occurring anomaly */
        private TDoubleList historicalLikelihoods;
        
        public State() {
            this(null, new Tuple(null, null, null), new TDoubleArrayList());
        }
        
        /**
         * Represents the current state a given {@link AnomalyLikelihood}
         * 
         * @param s                         {@link Statistic}, containing original value, mean, standard deviation
         * @param movingAverage             {@link Tuple} of historicalValues, total, windowSize
         * @param historicalLikelihoods     computed list of anomaly likelihoods
         */
        public State(Statistic s, Tuple movingAverage, TDoubleList historicalLikelihoods) {
            this.distribution = s;
            this.movingAverage = movingAverage;
            this.historicalLikelihoods = historicalLikelihoods;
        }
        
        public String toJson() {
            // Create the node factory that gives us nodes.
            JsonNodeFactory factory = new JsonNodeFactory(false);
     
            // create a json factory to write the tree node as json. for the example
            // we just write to console
            JsonFactory jsonFactory = new JsonFactory();
            JsonGenerator generator = null;
            StringWriter out = new StringWriter();
            try {
                 generator = jsonFactory.createGenerator(out);
            }catch(IOException e) {
                LOG.error("Error while creating JsonGenerator", e);
            }
            ObjectMapper mapper = new ObjectMapper();
     
            // the root node - state
            ObjectNode state = factory.objectNode();
            state.set("distribution", distribution.toJson(factory));
            
            
            
            try {
                mapper.writeTree(generator, state);
            } catch(JsonProcessingException e) {
                LOG.error("Error while writing json", e);
            } catch(IOException e) {
                LOG.error("Error while writing json", e);
            }
            
            return out.getBuffer().toString();
        }
    }
}
