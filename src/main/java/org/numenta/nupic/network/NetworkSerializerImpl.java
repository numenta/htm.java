/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.Persistable;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>
 * Low level serializer that wraps an FST serialization scheme in both a Kryo {@link Serializer}
 * and an HTM.java {@link NetworkSerializer} interface.</p>
 * <p>
 * This implementation may be used with a Kryo serialization scheme as it implements {@link Serializer} 
 * which is necessary to interface with systems that use Kryo as a serialization method (i.e. Flink).
 * The {@link #getKryo()} method is used to retrieve an instance of Kryo which can be used with such
 * frameworks and contains the required delegation to the internal serialization scheme used by the
 * HTM.java framework. 
 * </p><p>
 * To obtain an instance of this serializer see {@link Network#serializer(SerialConfig, boolean)}
 * </p>
 *  
 * @author cogmission
 *
 * @param <T>   the type which will be serialized
 */
class NetworkSerializerImpl<T extends Persistable> extends Serializer<T> implements NetworkSerializer<T>, Persistable {
    private static final long serialVersionUID = 1L;

    protected static final Logger LOGGER = LoggerFactory.getLogger(NetworkSerializerImpl.class);
    
    /** Time stamped serialization file format */
    public static transient DateTimeFormatter CHECKPOINT_TIMESTAMP_FORMAT = DateTimeFormat.forPattern(SerialConfig.CHECKPOINT_FORMAT_STRING);
    private transient DateTimeFormatter checkPointFormatter = CHECKPOINT_TIMESTAMP_FORMAT;
    private String checkPointFormatString;
    
    /** Use of Fast Serialize https://github.com/RuedigerMoeller/fast-serialization */
    private final FSTConfiguration fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
    
    /** Kryo serializer */
    private Kryo kryo;
    
    /** Indicates the underlying serialization scheme to use see {@link NetworkSerializer.Scheme} */
    private SerialConfig config;
    private Scheme scheme;
    
    /** Configured from the specified {@link SerialConfig} passed in. */
    private File serializedFile;
    
    /** Stores the bytes of the last serialized object or null if there was a problem */
    private static AtomicReference<byte[]> lastBytes = new AtomicReference<byte[]>(null);
    
    
    
    /** 
     * All instances in this classloader will share the same atomic reference to the last checkpoint file name holder
     * which is perfectly fine.
     */
    private static AtomicReference<String> lastCheckPointFileName = new AtomicReference<String>(null);
    
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private Lock writeMonitor = rwl.writeLock();
    
    /**
     * Constructs a new {@code NetworkSerializerImpl} which will use the specified
     * underlying serialization scheme.
     * 
     * @param config      the configuration to use
     */
    NetworkSerializerImpl(SerialConfig config) {
        this.config = config;
        this.scheme = config.getScheme();
        this.checkPointFormatString = config.getCheckPointFormatString();
        this.checkPointFormatter = DateTimeFormat.forPattern(this.checkPointFormatString);
        
        if(config.getScheme() == Scheme.KRYO) {
            this.kryo = createKryo();
        }
        
        fastSerialConfig.registerClass(config.getRegistry().toArray(new Class[config.getRegistry().size()]));
    }
    
    /**
     * <b>Warning: Do not use!</b>
     * To only be called by a <a href="https://ci.apache.org/projects/flink/flink-docs-master/
     * apis/best_practices.html#register-a-custom-serializer-for-your-flink-program">Flink</a>
     * cluster server. 
     * 
     * Use instead {@link #deSerialize(Class)}
     * 
     * @param kryo      instance of {@link Kryo} object
     * @param in        a Kryo {@link Input} 
     * @param klass     The class of the object to be read in.
     * @return  an instance of type &lt;T&gt;
     */
    @SuppressWarnings("unchecked")
    @Override
    public T read(Kryo kryo, Input in, Class<T> klass) {
        FSTObjectInput reader = fastSerialConfig.getObjectInput(in);
        try {
            T t = (T) reader.readObject(klass);

            if(t instanceof Persistable) {
                ((Persistable) t).postDeSerialize();
            }
            return t;
        }
        catch(Exception e) {
            throw new KryoException(e);
        }
    }
    
    /**
     * <b>Warning: Do not use!</b>
     * To only be called by a <a href="https://ci.apache.org/projects/flink/flink-docs-master/
     * apis/best_practices.html#register-a-custom-serializer-for-your-flink-program">Flink</a>
     * cluster server. 
     * 
     * Use instead {@link #serialize(Object)}
     * 
     * @param kryo      instance of {@link Kryo} object
     * @param out       a Kryo {@link Output} object        
     * @param <T>       instance to serialize     
     */
    @Override
    public void write(Kryo kryo, Output out, T t) {
        try {
            if(t instanceof Persistable) {
                ((Persistable) t).preSerialize();
            }
            
            byte[] bytes = fastSerialConfig.asByteArray(t);
            out.write(bytes);
        }
        catch(Exception e) {
            throw new KryoException(e);
        }
    }
    
    /**
     * Returns the {@link SerialConfig} in use currently.
     * @return      the {@link SerialConfig} in use currently.
     */
    @Override
    public SerialConfig getConfig() {
        return config;
    }
    
    /**
     * Writes the current state of the specified object to disk. This method is intended for
     * {@link Network} serialization to disk. For other objects, use the more general {@link #serialize(Persistable)}
     * and {@link #deSerialize(Class, byte[])} methods.
     * 
     *  @param  <T>     instance of {@link Network} to check point.
     *  @return byte[]  the serialized bytes of the specified Network.
     */
    @Override
    public byte[] checkPoint(T instance) {
        instance.preSerialize();
        
        String path = System.getProperty("user.home") + File.separator + SERIAL_DIR;
        File customDir = new File(path);
        
        customDir.mkdirs();
        
        String oldCheckPointFileName = lastCheckPointFileName.getAndSet(
            customDir.getAbsolutePath() + File.separator + config.getCheckPointFileName() + 
                checkPointFormatter.print(new DateTime()));

        File cpFile = new File(lastCheckPointFileName.get());
        try {
            cpFile.createNewFile();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
        
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                writeMonitor.lock();
                byte[] bytes = fastSerialConfig.asByteArray(instance);
                try {
                    Files.write(cpFile.toPath(), bytes, config.getCheckPointOpenOptions());
                    
                    // If the user has specified to remove old checkpoint files, delete the last one.
                    if(config.isOneCheckPointOnly() && oldCheckPointFileName != null) {
                        Files.deleteIfExists(new File(oldCheckPointFileName).toPath());
                    }
                } catch(IOException e) {
                    lastBytes.set(null);
                    throw new RuntimeException(e);
                }finally{
                    writeMonitor.unlock();
                }
                
                lastBytes.set(bytes);
                return lastBytes.get();
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to serialize the specified Object instance. In all cases, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance  the object instance to serialize
     * @return  the instance of type &lt;T&gt; serialized to a byte array
     */
    @Override
    public byte[] serialize(T instance) {
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                return serialize(instance, false);
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to serialize the specified Object instance. In all cases, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance      the object instance to serialize
     * @param bytesOnly     flag indicating whether to persist to disk or permanent storage. 
     *                      <ul>
     *                          <li><b>If true</b>, this
     *                      serializer will not persist to disk and only return the serialized bytes(bytesOnly==true);</li>
     *                          <li><b>if false</b>, the serialized bytes will still be returned, but the object will also
     *                      be saved to disk (perm storage: bytesOnly==false).</li>
     *                      </ul> 
     *                      
     * @return the instance of type &lt;T&gt; serialized to a byte array
     */
    @Override
    public byte[] serialize(T instance, boolean bytesOnly) {
        // Make sure any serialized Network is first halted.
        instance.preSerialize();
        
        try {
            ensurePathExists(config);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                writeMonitor.lock();
                
                byte[] bytes = null;
                try {
                    bytes = fastSerialConfig.asByteArray(instance);
                    
                    if(!bytesOnly) {
                        Files.write(serializedFile.toPath(), bytes, config.getOpenOptions());
                    }
                } catch(IOException e) {
                    lastBytes.set(null);
                    throw new RuntimeException(e);
                }finally{
                    writeMonitor.unlock();
                }
                
                lastBytes.set(bytes);
                return lastBytes.get();
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to deserialize the specified Class type. If the scheme was previously set to {@link Scheme#FST}, a byte
     * array may be specified, from which the deserialized object will be returned. Otherwise, the object returned
     * is retrieved from the previously configured file.
     * 
     * @param type      Class indicating which Object to deserialize
     * @param bytes     <b>(Optional - may be null)</b> 
     *                  <ul>
     *                      <li><b>If present (bytes!=null)</b>: simply deserialize the supplied bytes into the
     *                      requested object</li>
     *                      <li><b>If null (bytes==null)</b>: recover the serialized object from disk using the 
     *                      path specified by the pre-configured {@link SerialConfig} object.</li>
     *                  </ul>
     * @return  the deserialized object of type &lt;T&gt;
     */
    @SuppressWarnings("unchecked")
    @Override
    public T deSerialize(Class<T> type, byte[] bytes) {
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                if(bytes == null) {
                    try {
                        bytes = Files.readAllBytes(serializedFile.toPath());
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                T retVal = (T)fastSerialConfig.asObject(bytes);
                return retVal.postDeSerialize();
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to deserialize the specified Class type using the specified file name (minus path for usual case,
     * though you may specify a "sub" path by prepending a path to the filename).
     * 
     * @param type          the subclass of {@link Persistable} to be deserialized.
     * @param fileName      the name of the file containing the serialized Persistable to be returned.
     * @return  the serialized Persistable
     * @throws IOException      if the file doesn't exist
     */
    @SuppressWarnings("unchecked")
    public T deSerializeFromFile(Class<T> type, String fileName) throws IOException {
        File serializedFile = null;
        try {
            serializedFile = testFileExists(fileName);
        }catch(FileNotFoundException f) {
            return null;
        }
        
        byte[] bytes = null;
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                if(bytes == null) {
                    try {
                        bytes = Files.readAllBytes(serializedFile.toPath());
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                T retVal = (T)fastSerialConfig.asObject(bytes);
                return retVal.postDeSerialize();
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * Returns the bytes of the last serialized object or null if there was a problem.
     * @return  the bytes of the last serialized object.
     */
    @Override
    public byte[] getLastBytes() {
        return lastBytes.get();
    }
    
    /**
     * Returns the File object that the serialization scheme writes to.
     * @return  the {@link File} object containing the serialized object
     */
    @Override
    public File getSerializedFile() {
        return serializedFile;
    }
    
    /**
     * Returns the last check pointed file name.
     * @return  the last check pointed file name.
     */
    @Override
    public String getLastCheckPointFileName() {
        return lastCheckPointFileName.get();
    }
    
    /**
     * Returns a list of all the checkpointed files.
     * 
     * @return  list of all the checkpointed files.
     */
    @Override
    public List<String> getCheckPointFileList() {
        String path = System.getProperty("user.home") + File.separator + SERIAL_DIR;
        File customDir = new File(path);
        
        final DateTimeFormatter f = checkPointFormatter;
        List<String> chkPntFiles = Arrays.stream(customDir.list((d,n) -> {
            // Return only checkpoint files before the specified checkpoint name.
            return n.indexOf(config.getCheckPointFileName()) != -1;
        })).sorted((o1,o2) -> {
            // Sort the list so that the most recent previous can be selected.
            try {
                String n1 = o1.substring(config.getCheckPointFileName().length());
                String n2 = o2.substring(config.getCheckPointFileName().length());
                return f.parseDateTime(n1).compareTo(f.parseDateTime(n2));
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }).collect(Collectors.toList());
        
        return chkPntFiles;
    }
    
    /**
     * Returns the checkpointed file previous to the specified file (older), or
     * null if one doesn't exist. The file name may be the entire filename (as
     * configured by the {@link SerialConfig} object which establishes both the
     * filename portion and the date portion formatting), or just the date
     * portion of the filename.
     * 
     * @param   checkpoint filename (can be entire name or just date portion)
     * 
     * @return  the full filename of the file checkpointed immediately previous
     *          to the file specified.
     */
    @Override
    public String getPreviousCheckPoint(String checkPointFileName) {
        final DateTimeFormatter f = checkPointFormatter;
        
        String defaultNamePortion = config.getCheckPointFileName();
        
        if(checkPointFileName.indexOf(defaultNamePortion) != -1) {
            checkPointFileName = checkPointFileName.substring(defaultNamePortion.length());
        }
        
        final String cpfn = checkPointFileName;
        
        String[] chkPntFiles = getCheckPointFileList().stream()
            .map(n -> n.substring(defaultNamePortion.length()))
            .filter(n -> f.parseDateTime(n).isBefore(f.parseDateTime(cpfn)))
            .toArray(size -> new String[size]);
        
        if(chkPntFiles != null && chkPntFiles.length > 0) {
            return defaultNamePortion.concat(chkPntFiles[chkPntFiles.length - 1]);
        }
        
        return null;
    }
    
    /**
     * Returns the underlying {@link Kryo} impl.
     * @return the underlying {@link Kryo} impl.
     */
    @Override
    public Kryo getKryo() {
        return kryo;
    }
    
    /**
     * Creates the serialization file.
     * 
     * @param config            the {@link SerialConfig} specifying the file storage file.
     * @return                  flag indicating success
     * @throws IOException      if there is a problem locating the specified directories or 
     *                          creating the file.
     */
    boolean ensurePathExists(SerialConfig config) throws IOException {
        String path = System.getProperty("user.home") + File.separator + SERIAL_DIR;
        File customDir = new File(path);
        // Make sure container directory exists
        customDir.mkdirs();
        
        serializedFile = new File(customDir.getAbsolutePath() + File.separator +  config.getFileName());
        if(!serializedFile.exists()) {
            serializedFile.createNewFile();
        }
        
        return true;
    }
    
    /**
     * Returns the File corresponding to "fileName" if this framework is successful in locating
     * the specified file, otherwise it throws an {@link IOException}.
     * 
     * @param fileName          the name of the file to search for.
     * @return  the File if the operation is successful, otherwise an exception is thrown
     * @throws IOException      if the specified file is not found, or there's a problem loading it.
     */
    File testFileExists(String fileName) throws IOException, FileNotFoundException {
        try {
            String path = System.getProperty("user.home") + File.separator + SERIAL_DIR;
            File customDir = new File(path);
            // Make sure container directory exists
            customDir.mkdirs();
            
            File serializedFile = new File(customDir.getAbsolutePath() + File.separator +  fileName);
            if(!serializedFile.exists()) {
                throw new FileNotFoundException("File \"" + fileName + "\" was not found.");
            }
            
            return serializedFile;
        }catch(IOException io) {
            throw io;
        }
    }
    
    /**
     * Returns a {@link Kryo} implementation which adheres to the Kryo interface requirements
     * but is capable of delegating to FST :-P
     * @return  an instance of Kryo
     */
    private Kryo createKryo() {
        Kryo.DefaultInstantiatorStrategy initStrategy = new Kryo.DefaultInstantiatorStrategy();

        // use Objenesis to create classes without calling the constructor (Flink's technique)
        //initStrategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());

        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(initStrategy);
        return kryo;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((config == null) ? 0 : config.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        NetworkSerializerImpl<?> other = (NetworkSerializerImpl<?>)obj;
        if(config == null) {
            if(other.config != null)
                return false;
        } else if(!config.equals(other.config))
            return false;
        return true;
    }
}
