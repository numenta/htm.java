package org.numenta.nupic.examples.cortical;

import io.cortical.rest.model.Fingerprint;
import io.cortical.rest.model.Term;
import io.cortical.services.RetinaApis;
import io.cortical.services.Terms;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.numenta.nupic.datagen.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Test {
    private static final Logger LOGGER = LoggerFactory.getLogger(Test.class);
    
    private static final String RETINA_NAME = "en_associative";    
    private static final String RETINA_IP = "api.cortical.io";
    
    private static final double SDR_WIDTH = 16384D;
    
    private String apiKey = "";
    private String filePath;
    
    private List<String[]> input;
    
    private Terms api;
    
    
    /**
     * Constructs a new...
     * @param pathToSource
     */
    public Test(String pathToSource) {
        this.filePath = pathToSource;
        this.input = readFile(filePath);
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
            api = new RetinaApis(RETINA_NAME, RETINA_IP, this.apiKey).termsApi();
            System.out.println(Arrays.toString(api.getTerm("apple", false).get(0).getFingerprint().getPositions()));
            
            LOGGER.debug("Successfully initialized retinal api");
            
            return true;
        }catch(Exception e) {
            LOGGER.debug("Problem initializing retinal api");
            return false;
        }
    }
    
    private Term getClosestTerm(int[] sdr) {
        api.get
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
            return api.getTerm(term, true).get(0).getFingerprint();
        }catch(Exception e) {
            LOGGER.debug("Problem retrieving fingerprint for term: " + term);
        }
        
        return null;
    }
    
    /**
     * Returns an {@link Iterator} over the list of string arrays (lines)
     * @return
     */
    private Iterator<String[]> iterator() {
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
        
        List<String[]> list = stream.map(l -> { return (String[])l.split("[\\s]+\\,[\\s]+"); }).collect(Collectors.toList());
        
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
    
    public static void main(String[] args) {
        Test test = new Test("foxeat.csv");
        boolean success = test.connectionValid("dd");//"d059e560-1372-11e5-a409-7159d0ac8188");
        System.out.println("success = " + success);
        
        success = test.connectionValid("d059e560-1372-11e5-a409-7159d0ac8188");
        System.out.println("success = " + success);
        
        int[] sdr = test.getFingerprintSDR("apple");
        System.out.println("sdr = " + sdr);
        
        int sparsity = test.getSparsity(sdr);
        System.out.println("sparsity = " + sparsity + "%");
        
        
    }
}
