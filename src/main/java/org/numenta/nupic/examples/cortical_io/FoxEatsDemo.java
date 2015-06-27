package org.numenta.nupic.examples.cortical_io;

import gnu.trove.list.array.TIntArrayList;
import io.cortical.rest.model.Fingerprint;
import io.cortical.rest.model.Term;
import io.cortical.services.Expressions;
import io.cortical.services.RetinaApis;
import io.cortical.services.Terms;
import io.cortical.services.api.client.ApiException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This the HTM.java version of Subutai Ahmad's Hackathon Demo illustrating the 
 * integration of HTM technology; Cortical.io's Natural Language Programming (NLP)
 * technology; and the HTM.java Network API.
 * 
 * This demo demonstrates the powerful similarity and word association technology of
 * Cortial.io and the generalization power of HTM technology by "teaching" the HTM triplets
 * of animals, actions, and objects such as "frog eats flies", and "cow eat grain". After
 * presenting the HTM with 36 different examples of triplet animal "preferences", we then
 * ask the HTM what a "fox" would eat.
 * 
 * The HTM, having never "seen" the word fox before, comes back with "rodent" or "squirrel",
 * which is what an animal that is "fox-like" might eat. Cortical.io's "Semantic Folding" 
 * technology, utilizes SDRs (Sparse Data Representations) to encode property qualities to
 * sparse data bits. The use of this technology to reverse engineer the HTM's "prediction"
 * to see what "meal" the HTM generalizes for foxes, exhibits vast potential in the combination
 * of these two advanced Machine Intelligence technologies. 
 * 
 * @author cogmission
 *
 */
public class FoxEatsDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(FoxEatsDemo.class);
    
    private static final String RETINA_NAME = "en_associative";    
    private static final String RETINA_IP = "api.cortical.io";
    
    private static final double SDR_WIDTH = 16384D;
    private static final double SPARSITY = 0.02D;
    
    static String cachePath = System.getProperty("user.home").concat(File.separator).
        concat(".cortical").concat(File.separator).concat("cache");
    
    private static final Random RANDOM = new MersenneTwister(42);
    
    private String apiKey = "";
    private String filePath;
    
    private List<String[]> input;
    private Map<String, Term> cache;
    
    private Terms termsApi;
    private Expressions exprApi;
    
    private Network network;
    
    
    /**
     * Constructs a new Network API demo to demonstrate interaction with
     * the Cortical.io API specifically.
     * 
     * @param pathToSource
     */
    public FoxEatsDemo(String pathToSource) {
        this.filePath = pathToSource;
        this.input = readInputData(filePath);
    }
    
    /**
     * Used for testing to set the cache file path
     * @param path
     */
    void setCachePath(String path) {
        cachePath = path;
    }
    
    /**
     * Returns the cache {@link File} specified by the pre-configured file path.
     * (see {@link #Demo(String)}) The cache file is by default stored in the user's
     * home directory in the ".cortical/cache" file. This file stores the {@link Term}
     * objects retrieved via the Cortical.io API so that the server is "pounded" by 
     * queries from this demo.
     * 
     * @return
     */
    File getCacheFile() {
        File f = new File(cachePath);
        if(!f.exists()) {
            try {
                new File(cachePath.substring(0, cachePath.lastIndexOf(File.separator))).mkdir();
                f.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("Unable to write cache file.");
            }
            
            LOGGER.debug("Created cache file: " + cachePath);
        }
        return f;
    }
    
    /**
     * Returns the {@link Fingerprint} cache.
     * @return
     */
    Map<String, Term> getCache() {
        return cache;
    }
    
    /**
     * Returns the persisted cache as a {@link Stream}
     * @return
     */
    Stream<String> getCacheStream() throws IOException {
        File f = getCacheFile();
        Stream<String> stream = Files.lines(f.toPath());
        
        return stream;
    }
    
    /**
     * Loads the fingerprint cache file into memory if it exists. If it
     * does not exist, this method creates the file; however it won't be
     * written to until the demo is finished processing, at which point
     * {@link #writeCache()} is called to store the cache file.
     */
    void loadCache() {
        if(cache == null) {
            cache = new HashMap<>();
        }
        
        String json = null;
        try {
            StringBuilder sb = new StringBuilder();
            getCacheStream().forEach(l -> { sb.append(l); });
            json = sb.toString();
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        if(json.isEmpty()) {
            LOGGER.debug("Term cache is empty.");
            return;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        List<Term> terms = null;
        try {
            terms = Arrays.asList(mapper.readValue(json, Term[].class));
            if(terms == null) {
                LOGGER.debug("Term cache is empty or malformed.");
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        for(Term t : terms) {
            cache.put(t.getTerm(), t);
        }
        
        checkCache(true, true);
    }
    
    /**
     * Writes the fingerprint cache file to disk. This takes place at the
     * end of the demo's processing.
     */
    void writeCache() {
        File f = getCacheFile();
        
        try(PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            StringBuilder builderStr = new StringBuilder();
            int i = 0;
            for(Iterator<Term> it = cache.values().iterator();it.hasNext();i++) {
                Term t = it.next();
                String termStr = Term.toJson(t);
                if(i > 0) {
                    termStr = termStr.substring(1).trim();
                }
                termStr = termStr.substring(0, termStr.length() - 1).trim();
                builderStr.append(termStr).append(",");
            }
            builderStr.setLength(builderStr.length() - 1);
            builderStr.append(" ]");
            
            pw.println(builderStr.toString());
            pw.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Called to check the coherence of the cache file to make sure there
     * is no missing data and that the fingerprint {@link Term}s stored internally are
     * valid.
     * 
     * @param print         flag indicating whether the check should be logged
     * @param failOnCheck   flag indicating whether a failed cache entry should cause this
     *                      demo to exit. 
     */
    void checkCache(boolean print, boolean failOnCheck) {
        int count = 0;
        for(String key : cache.keySet()) {
            if(print) { LOGGER.debug((count++) + ". key: " + key); }
            
            if(!checkTerm(key, cache.get(key), print)) {
                if(failOnCheck) {
                    throw new IllegalStateException("Term cache for key: " + key + " was invalid or missing data.");
                }
            }
        }
    }
    
    /**
     * Called by {@link #checkCache(boolean, boolean)} for every line in
     * the cache file to validate the contents of the specified {@link Term}
     * 
     * @param key
     * @param t
     * @param print
     * @return
     */
    boolean checkTerm(String key, Term t, boolean print) {
        Fingerprint fp = t.getFingerprint();
        if(fp == null) {
            if(print) { LOGGER.debug("\tkey: " + key + ", missing fingerprint"); }
            return false;
        }
        
        int[] pos = fp.getPositions();
        if(pos == null) {
            if(print) { LOGGER.debug("\tkey: " + key + ", has null positions"); }
            return false;
        }
        
        if(pos.length < 1) {
            if(print) { LOGGER.debug("\tkey: " + key + ", had empty positions"); }
            return false;
        }
        
        int sdrLen = pos.length;
        if(print) {
            LOGGER.debug("\tkey: " + key + ", term len: " + sdrLen);
        }
        
        return true;
    }
    
    /**
     * Initializes the Retina API end point, 
     * and returns a flag indicating whether the apiKey
     * is valid and a connection has been configured.
     * 
     * @param apiKey
     */
    boolean connectionValid(String apiKey) {
        try {
            this.apiKey = apiKey;
            RetinaApis ra = new RetinaApis(RETINA_NAME, RETINA_IP, this.apiKey);
            termsApi = ra.termsApi();
            exprApi = ra.expressionsApi();
            System.out.println(Arrays.toString(termsApi.getTerm("apple", false).get(0).getFingerprint().getPositions()));
            
            LOGGER.debug("Successfully initialized retinal api");
            
            return true;
        }catch(Exception e) {
            LOGGER.debug("Problem initializing retinal api");
            return false;
        }
    }
    
    /**
     * Returns a list of {@link Term}s using the Terms API.
     * 
     * @param term  a term.
     * @param includeFingerprint    true if call should return the positions array,
     *                              false if not.
     * @return
     * @throws ApiException
     * @throws JsonProcessingException
     */
    List<Term>  getTerms(String term, boolean includeFingerprint) throws ApiException, JsonProcessingException {
        return termsApi.getTerm(term, includeFingerprint);
    }
    
    /**
     * Returns a list of similar terms using the Expressions API.
     * 
     * @param fp    a Fingerprint from which to get similar terms.
     * @return  List view of similar {@link Term}s
     * @throws ApiException
     * @throws JsonProcessingException
     */
    List<Term> getSimilarTerms(Fingerprint fp) throws ApiException, JsonProcessingException {
        return exprApi.getSimilarTerms(fp);
    }
    
    /**
     * Returns the most similar {@link Term} for the term represented by
     * the specified sdr
     * 
     * @param sdr   sparse int array
     * @return
     */
    Term getClosestTerm(int[] sdr) {
        Fingerprint fp = new Fingerprint(sdr);
        try {
            List<Term> terms = getSimilarTerms(fp);
            
            // Retrieve terms from cache if present
            for(int i = 0;i < terms.size();i++) {
                if(cache.containsKey(terms.get(i).getTerm())) {
                    terms.set(i, cache.get(terms.get(i).getTerm()));
                }
            }
            
            Term retVal = null;
            if(terms != null && terms.size() > 0) {
                retVal = terms.get(0);
                if(checkTerm(retVal.getTerm(), retVal, true)) {
                    return retVal;
                }
                
                // Cache fall through incomplete term for next time
                cache.put(retVal.getTerm(), retVal = getTerms(retVal.getTerm(), true).get(0));
                return retVal;
            }
        }catch(Exception e) {
            LOGGER.debug("Problem using Expressions API");
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Returns the percent sparsity of the specified SDR.
     * 
     * @param sdr
     * @return
     */
    int getSparsity(int[] sdr) {
        double sparsity = ((double)sdr.length / SDR_WIDTH) * 100;
        return (int)sparsity;
    }
    
    /**
     * Returns the randomly culled array whose entries are equal
     * to {@link #SPARSITY}
     * 
     * @param input     the int array to ensure proper sparsity for.
     * @return
     */
    int[] subsample(int[] input) {
        int sparsity = getSparsity(input);
        if(sparsity > 2) {
           input = ArrayUtils.sample((int)(SDR_WIDTH * SPARSITY) + 1, new TIntArrayList(input), RANDOM); 
        }
        return input;
    }
    
    /**
     * Returns the SDR which is the sparse integer array
     * representing the specified term.
     * 
     * @param term
     * @return
     */
    int[] getFingerprintSDR(String term) {
        return getFingerprintSDR(getFingerprint(term));
    }
    
    /**
     * Returns the SDR which is the sparse integer array
     * representing the specified term.
     * 
     * @param fp
     * @return
     */
    int[] getFingerprintSDR(Fingerprint fp) {
        return fp.getPositions();
    }
    
    /**
     * Returns a {@link Fingerprint} for the specified term.
     * 
     * @param term
     * @return
     */
    Fingerprint getFingerprint(String term) {
        try {
            Term t = cache.get(term) == null ?
                getTerms(term, false).get(0) :
                    cache.get(term);
                
            if(!checkTerm(t.getTerm(), t, true)) {
                System.exit(1);
            }
                
            cache.put(t.getTerm(), t);
            
            return t.getFingerprint();
        }catch(Exception e) {
            LOGGER.debug("Problem retrieving fingerprint for term: " + term);
        }
        
        return null;
    }
    
    /**
     * Returns an {@link Iterator} over the list of string arrays (lines)
     * @return
     */
    Iterator<String[]> inputIterator() {
        return input.iterator();
    }
    
    /**
     * Returns a {@link List} of String arrays, each representing
     * a line of text.
     * 
     * @param pathToFile
     * @return
     */
    List<String[]> readInputData(String pathToFile) {
        Stream<String> stream = getInputDataStream(pathToFile);
        
        List<String[]> list = stream.map(l -> { return (String[])l.split("[\\s]*\\,[\\s]*"); }).collect(Collectors.toList());
        
        return list;
    }
    
    /**
     * Returns a Stream from the specified file path
     * 
     * @param pathToFile
     * @return
     */
    Stream<String> getInputDataStream(String pathToFile) {
        File inputFile = null;
        Stream<String> retVal = null;
        
        if(pathToFile.indexOf(File.separator) != -1) {
            inputFile = new File(pathToFile);
        }else{
            String path = ResourceLocator.path(pathToFile);
            if(path.indexOf("!") != -1) {
                path = path.substring("file:".length());
                return FileSensor.getJarEntryStream(path);
            }else{
                inputFile = new File(path);
            }
        }
        
        try {
            if(!inputFile.exists()) {
                throw new FileNotFoundException(pathToFile);
            }
            
            retVal = Files.lines(inputFile.toPath(), Charset.forName("UTF-8"));
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        return retVal;
    }
    
    /**
     * Creates and returns a {@link Parameters} object configured
     * for this demo.
     * 
     * @return
     */
    Parameters createParameters() {
        Parameters tmParams = Parameters.getTemporalDefaultParameters();
        tmParams.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 16384 });
        tmParams.setParameterByKey(KEY.CELLS_PER_COLUMN, 8);
        tmParams.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.5);
        tmParams.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.4);
        tmParams.setParameterByKey(KEY.MIN_THRESHOLD, 164);
        tmParams.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 164);
        tmParams.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.1);
        tmParams.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.0);
        tmParams.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 164);
        
        return tmParams;
    }
    
    /**
     * Creates and returns a {@link Network} for demo processing
     * @return
     */
    Network createNetwork() {
        Parameters temporalParams = createParameters();
        network = Network.create("Cortical.io API Demo", temporalParams)
            .add(Network.createRegion("Region 1")
                .add(Network.createLayer("Layer 2/3", temporalParams)
                    .add(new TemporalMemory())));
        return network;            
    }
    
    /**
     * Feeds the specified {@link Network} with the contents of the specified
     * {@link Iterator}
     * @param network   the current {@link Network} object
     * @param it        an {@link Iterator} over the input source file lines
     * @return
     */
    String[] feedNetwork(Network network, Iterator<String[]> it) {
        for(;it.hasNext();) {
            String[] next = it.next();
            
            if(!it.hasNext()) return next;
            
            for(String term : next) {
                int[] sdr = getFingerprintSDR(term);
                network.compute(sdr);
            }
            network.reset();
        }
        
        return null;
    }
    
    /**
     * Feeds the {@link Network with the final phrase consisting of the first
     * two words of the final question "fox, eats, ...".
     * 
     * @param network   the current {@link Network} object
     * @param it        an {@link Iterator} over the input source file lines
     * @return
     */
    Term feedQuestion(Network network, String[] phrase) {
        for(int i = 0;i < 2;i++) {
            int[] sdr = getFingerprintSDR(phrase[i]);
            network.compute(sdr);
        }
        
        int[] prediction = network.lookup("Region 1").lookup("Layer 2/3").getPredictedColumns();
        Term term = getClosestTerm(prediction);
        cache.put(term.getTerm(), term);
        
        return term;
    }
    
    public static void main(String[] args) {
        // Check for the existence of a proper API Key
        if(args.length < 1 || !args[0].startsWith("-K")) {
            throw new IllegalStateException("Demo must be started with arguments [-K]<your-api-key>");
        }
        
        // Extract api key from arguments
        String apiKey = args[0].substring(2).trim();
        
        // Instantiate the Demo
        FoxEatsDemo demo = new FoxEatsDemo("foxeat.csv");
        demo.loadCache();
        
        // Test api connection by executing dummy query
        boolean success = demo.connectionValid(apiKey);
        if(!success) {
            throw new RuntimeException(new ApiException());
        }

        // Create the Network
        Network network = demo.createNetwork();

        // Returns the last line of the file which is has the question terms: "fox, eats, <something>"
        String[] question = demo.feedNetwork(network, demo.inputIterator());
        
        // Returns the Term for the answer to what a fox eats.
        Term answer = demo.feedQuestion(network, question);
        
        // Print it to standard out. (For now...)
        System.out.println("What does a fox eat? Answer: " + answer.getTerm());
        
        // Cache fingerprints
        demo.writeCache();
        
    }
}
