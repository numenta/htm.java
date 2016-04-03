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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.Persistable;
import org.nustaq.serialization.FSTConfiguration;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;

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
class NetworkSerializerImpl<T extends Persistable> extends Serializer<T> implements NetworkSerializer<T> {
    /** Time stamped serialization file format */
    public static final DateTimeFormatter CHECKPOINT_TIMESTAMP_FORMAT = DateTimeFormat.forPattern(SerialConfig.CHECKPOINT_FORMAT_STRING);
    
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
    private byte[] lastBytes;
    
    private DateTimeFormatter checkPointFormatter = CHECKPOINT_TIMESTAMP_FORMAT;
    
    private String checkPointFormatString;
    
    private String lastCheckPointFileName;
    
    
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
    @Override
    public T read(Kryo kryo, Input in, Class<T> klass) {
        InputStream is = in.getInputStream();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
              buffer.write(data, 0, nRead);
            }
            buffer.flush();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
        
        return deSerialize(klass, buffer.toByteArray());
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
    public void write(Kryo kryo, Output out, T type) {
        byte[] bytes = serialize(type);
        out.write(bytes);
        
        lastBytes = bytes;
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
        
        String oldCheckPointFileName = lastCheckPointFileName;
        lastCheckPointFileName = customDir.getAbsolutePath() + File.separator +  
            config.getCheckPointFileName() + checkPointFormatter.print(new DateTime());
        
        File cpFile = new File(lastCheckPointFileName);
        try {
            cpFile.createNewFile();
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
        
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                byte[] bytes = fastSerialConfig.asByteArray(instance);
                try {
                    Files.write(cpFile.toPath(), bytes, config.getCheckPointOpenOptions());
                    
                    // If the user has specified to remove old checkpoint files, delete the last one.
                    if(config.isOneCheckPointOnly() && oldCheckPointFileName != null) {
                        Files.deleteIfExists(new File(oldCheckPointFileName).toPath());
                    }
                } catch(IOException e) {
                    lastBytes = null;
                    throw new RuntimeException(e);
                }
                return lastBytes = bytes;
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
                byte[] bytes = fastSerialConfig.asByteArray(instance);
                if(!bytesOnly) {
                    try {
                        Files.write(serializedFile.toPath(), bytes, config.getOpenOptions());
                    } catch(IOException e) {
                        lastBytes = null;
                        throw new RuntimeException(e);
                    }
                }
                return lastBytes = bytes;
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
                return retVal.postSerialize();
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
        return lastBytes;
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
        return lastCheckPointFileName;
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
        
        customDir.mkdirs();
        
        serializedFile = new File(customDir.getAbsolutePath() + File.separator +  config.getFileName());
        if(!serializedFile.exists()) {
            serializedFile.createNewFile();
        }
        
        return true;
    }
    
    /**
     * Returns a {@link Kryo} implementation which adheres to the Kryo interface requirements
     * but is capable of delegating to FST :-P
     * @return  an instance of Kryo
     */
    private Kryo createKryo() {
        return new KryoReflectionFactorySupport() {
            private NetworkSerializer<?> ser;
            private SerialConfig conf;
            
            {
                conf = new SerialConfig(
                    config.getFileName(), Scheme.FST, 
                        config.getRegistry(), config.getOpenOptions());
                ser = Network.serializer(conf, true);
            }
            
            @SuppressWarnings("rawtypes")
            @Override public Serializer<?> getDefaultSerializer(final Class clazz) {
                return (Serializer<?>)ser;
            }
        };
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
