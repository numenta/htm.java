package org.numenta.nupic.examples.cortical;

import io.cortical.rest.model.Fingerprint;
import io.cortical.rest.model.Term;
import io.cortical.services.Expressions;
import io.cortical.services.RetinaApis;
import io.cortical.services.Terms;

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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.research.TemporalMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;


public class Demo {
    private static final Logger LOGGER = LoggerFactory.getLogger(Demo.class);
    
    private static final String RETINA_NAME = "en_associative";    
    private static final String RETINA_IP = "api.cortical.io";
    
    private static final double SDR_WIDTH = 16384D;
    
    private static final String cachePath = System.getProperty("user.home") + File.separator + ".cortical" + File.separator + "cache";
    
    private String apiKey = "";
    private String filePath;
    
    private List<String[]> input;
    private Map<String, Term> cache;
    
    private Terms termsApi;
    private Expressions exprApi;
    
    private Subscriber<Inference> subscriber;
    private Network network;
    
    
    /**
     * Constructs a new...
     * @param pathToSource
     */
    public Demo(String pathToSource) {
        this.filePath = pathToSource;
        this.input = readFile(filePath);
    }
    
    private File getCacheFile() {
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
    
    private void loadCache() {
        if(cache == null) {
            cache = new HashMap<>();
        }
        
        File f = getCacheFile();
        
        String json = null;
        try {
            StringBuilder sb = new StringBuilder();
            Files.lines(f.toPath()).forEach(l -> { sb.append(l); });
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
    }
    
    private void writeCache() {
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
     * Initializes the Retina API end point, 
     * and returns a flag indicating whether the apiKey
     * is valid and a connection has been configured.
     * 
     * @param apiKey
     */
    private boolean connectionValid(String apiKey) {
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
     * Returns the most similar {@link Term} for the term represented by
     * the specified sdr
     * 
     * @param sdr   sparse int array
     * @return
     */
    private Term getClosestTerm(int[] sdr) {
        Fingerprint fp = new Fingerprint(sdr);
        try {
            List<Term> terms = exprApi.getSimilarTerms(fp);
            if(terms != null && terms.size() > 0) {
                return terms.get(0);
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
    private int getSparsity(int[] sdr) {
        double sparsity = ((double)sdr.length / SDR_WIDTH) * 100;
        return (int)sparsity;
    }
    
    /**
     * Returns the SDR which is the sparse integer array
     * representing the specified term.
     * 
     * @param term
     * @return
     */
    private int[] getFingerprintSDR(String term) {
        return getFingerprintSDR(getFingerprint(term));
    }
    
    /**
     * Returns the SDR which is the sparse integer array
     * representing the specified term.
     * 
     * @param fp
     * @return
     */
    private int[] getFingerprintSDR(Fingerprint fp) {
        return fp.getPositions();
    }
    
    /**
     * Returns a {@link Fingerprint} for the specified term.
     * 
     * @param term
     * @return
     */
    private Fingerprint getFingerprint(String term) {
        try {
            Term t = cache.get(term) == null ?
                termsApi.getTerm(term, true).get(0) :
                    cache.get(term);
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
    private Iterator<String[]> inputIterator() {
        return input.iterator();
    }
    
    /**
     * Returns a {@link List} of String arrays, each representing
     * a line of text.
     * 
     * @param pathToFile
     * @return
     */
    private List<String[]> readFile(String pathToFile) {
        Stream<String> stream = getFileStream(pathToFile);
        
        List<String[]> list = stream.map(l -> { return (String[])l.split("[\\s]*\\,[\\s]*"); }).collect(Collectors.toList());
        
        return list;
    }
    
    /**
     * Returns a Stream from the specified file path
     * 
     * @param pathToFile
     * @return
     */
    private Stream<String> getFileStream(String pathToFile) {
        File inputFile = null;
        Stream<String> retVal = null;
        
        if(pathToFile.indexOf(File.separator) != -1) {
            inputFile = new File(pathToFile);
        }else{
            inputFile = new File(ResourceLocator.path(pathToFile));
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
    private Parameters createParameters() {
        Parameters tmParams = Parameters.getTemporalDefaultParameters();
        tmParams.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 16384 });
        tmParams.setParameterByKey(KEY.CELLS_PER_COLUMN, 8);
        tmParams.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.5);
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
    private Network createNetwork() {
        Parameters temporalParams = createParameters();
        network = Network.create("Cortical.io API Demo", temporalParams)
            .add(Network.createRegion("Region 1")
                .add(Network.createLayer("Layer 2/3", temporalParams)
                    .add(new TemporalMemory())));
        return network;            
    }
    
    private Subscriber<Inference> createSubscriber() {
        return subscriber = new Subscriber<Inference>() {
            @Override public void onCompleted() {
                writeCache();
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                System.out.println("prediction = " + Arrays.toString(i.getSDR()));
            }
        };
    }
    
    private void subscribeToNetwork(Network network) {
        Subscriber<Inference> subscriber = createSubscriber();
        network.observe().subscribe(subscriber);
    }
    
    private void feedNetwork(Network network, Iterator<String[]> it) {
        for(;it.hasNext();) {
            for(String term : it.next()) {
                int[] sdr = getFingerprintSDR(term);
                network.compute(sdr);
            }
            network.reset();
        }
        
        //Notify onCompleted
        subscriber.onCompleted();
    }
    
    public static void main(String[] args) {
        Demo demo = new Demo("foxeat.csv");
        
        demo.loadCache();
        
        boolean success = demo.connectionValid("dd");//"d059e560-1372-11e5-a409-7159d0ac8188");
        System.out.println("success = " + success);
        
        success = demo.connectionValid("d059e560-1372-11e5-a409-7159d0ac8188");
        System.out.println("success = " + success);
        
        int[] sdr = demo.getFingerprintSDR("work");
        System.out.println("sdr = " + sdr);
        
        int sparsity = demo.getSparsity(sdr);
        System.out.println("sparsity = " + sparsity + "%");
        
        Term closest = demo.getClosestTerm(sdr);
        System.out.println("closest term = " + closest.getTerm());
        
        int count = 0;
        for(Iterator<String[]> it = demo.inputIterator();it.hasNext();it.next(),count++);
        System.out.println("count = " + count + ", " + demo.input.size());
        
        Network network = demo.createNetwork();
        demo.subscribeToNetwork(network);
        demo.feedNetwork(network, demo.inputIterator());
    }
}
