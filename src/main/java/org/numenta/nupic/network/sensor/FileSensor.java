/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.numenta.nupic.network.Network;

/**
 * Default implementation of a {@link Sensor} for inputting data from
 * a file.
 * 
 * All {@link Sensor}s represent the bottom-most level of any given {@link Network}. 
 * Sensors are used to connect to a data source and feed data into the Network, therefore
 * there are no nodes beneath them or which precede them within the Network hierarchy, in
 * terms of data flow. In fact, a Sensor will throw an {@link Exception} if an attempt to 
 * connect another node to the input of a node containing a Sensor is made.
 *  
 * @author David Ray
 * @see SensorFactory
 * @see Sensor#create(SensorFactory, SensorParams)
 */
public class FileSensor implements Sensor<File>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final int HEADER_SIZE = 3;
    private static final int BATCH_SIZE = 20;
    // This is OFF until Encoders are made concurrency safe
    private static final boolean DEFAULT_PARALLEL_MODE = false;
    
    private transient BatchedCsvStream<String[]> stream;
    private SensorParams params;
    
    /**
     * Private constructor. Instances of this class should be obtained 
     * through the {@link #create(SensorParams)} factory method.
     * 
     * @param params
     */
    private FileSensor(SensorParams params) {
        this.params = params;
        
        if(!params.hasKey("PATH")) {
            throw new IllegalArgumentException("Passed improperly formed Tuple: no key for \"PATH\"");
        }
        
        String pathStr = (String)params.get("PATH");

        if(pathStr.indexOf("!") != -1) {
            pathStr = pathStr.indexOf("file:") != -1 ? pathStr.substring("file:".length()) :
                pathStr;
            
            Stream<String> stream = getJarEntryStream(pathStr);
            this.stream = BatchedCsvStream.batch(
                stream, BATCH_SIZE, DEFAULT_PARALLEL_MODE, HEADER_SIZE);
        }else{
            File f = new File(pathStr);
            if(!f.exists()) {
                throw new IllegalArgumentException("Passed improperly formed Tuple: invalid PATH: " + params.get("PATH"));
            }
            
            try {
                Stream<String> stream = Files.lines(f.toPath(), Charset.forName("UTF-8"));
                this.stream = BatchedCsvStream.batch(
                    stream, BATCH_SIZE, DEFAULT_PARALLEL_MODE, HEADER_SIZE);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    /**
     * Factory method to allow creation through the {@link SensorFactory} in
     * the {@link Sensor#create(SensorFactory, SensorParams)} method of the 
     * parent {@link Sensor} class. This indirection allows the decoration of 
     * the returned {@code Sensor} type by wrapping it in an {@link HTMSensor}
     * (which is the current implementation but could be any wrapper).
     * 
     * @param p     the {@link SensorParams} which describe connection or source
     *              data details.
     * @return      the Sensor.
     */
    public static Sensor<File> create(SensorParams p) {
        Sensor<File> fs = new FileSensor(p);
        return fs;
    }
    
    /**
     * Returns this {@code FileSensor}'s {@link SensorParams}
     * @return  the SensorParams
     */
    @Override
    public SensorParams getSensorParams() {
        return params;
    }
    
    /**
     * Returns the configured {@link MetaStream} if this is of
     * Type Stream, otherwise it throws an {@link UnsupportedOperationException}
     * 
     * @return  the MetaStream
     */
    @SuppressWarnings("unchecked")
    @Override
    public <K> MetaStream<K> getInputStream() {
        return (MetaStream<K>)stream;
    }
    
    /**
     * Returns the values specifying meta information about the 
     * underlying stream.
     */
    public ValueList getMetaInfo() {
        return stream.getMeta();
    }

    /**
     * Returns a {@link Stream} from a Jar entry
     * @param path
     * @return
     */
    public static Stream<String> getJarEntryStream(String path) {
        Stream<String> retVal = null;
        String[] parts = path.split("\\!");
        try {
            JarFile jar = new JarFile(parts[0]);
            String innerPath = parts[1];
            innerPath = innerPath.startsWith("!") ? innerPath.substring(1) : innerPath;
            InputStream inStream = jar.getInputStream(jar.getEntry(innerPath));
            BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
            retVal = br.lines().onClose(() -> {
                try {
                    br.close();
                    jar.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }});
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        return retVal;
    }
    public static void main(String[] args) {
        String filepart = System.getProperty("user.home") + "/git/htm.java/src/test/resources/pathtest.jar";
        File f = new File(filepart);
        System.out.println("file exists ? " + f.exists());
        String path = filepart + "!rec-center-hourly.csv";
        Stream<String> stream = getJarEntryStream(path);
        stream.forEach(l -> System.out.println(l));
    }
}
