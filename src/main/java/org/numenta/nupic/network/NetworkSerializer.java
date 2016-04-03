package org.numenta.nupic.network;

import java.io.File;
import java.util.List;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public interface NetworkSerializer<T> {
    /** Currently there are two types of serialization implementations */
    public enum Scheme { KRYO, FST }
    
    /** The serialized file name */
    public static final String SERIAL_FILE_NAME = "Network.ser";
    
    /** The directory to store the serialized file */
    public static final String SERIAL_DIR = "HTMNetwork";
    
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
     * @return
     */
    public T read(Kryo kryo, Input in, Class<T> klass);
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
     * @return
     */
    public void write(Kryo kryo, Output out, T type);
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to serialize the specified Object instance. For both schemes, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance  the object instance to serialize
     */
    public byte[] serialize(T instance);
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to serialize the specified Object instance. For both schemes, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance      the object instance to serialize
     * @param bytesOnly     flag indicating whether to persist to disk or permanent storage.
     */
    public byte[] serialize(T instance, boolean bytesOnly);
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to deserialize the specified Class type. For both schemes, a byte
     * array may be specified, from which the deserialized object will be returned. Otherwise, the object returned
     * is retrieved from the previously configured file.
     * 
     * @param type      Class indicating which Object to deserialize
     * @param bytes     (Optional - may be null) for use with FST underlying scheme.
     * @return  the deserialized object
     */
    public T deSerialize(Class<T> type, byte[] bytes);
    /**
     * Returns the File object that the serialization scheme writes to.
     * @return
     */
    public File getSerializedFile();
    /**
     * Returns the last checkpointed file name.
     * @return  the last checkpointed file name.
     */
    public String getLastCheckPointFileName();
    /**
     * Returns a list of the current checkpoint files.
     * @return  checkpoint file list
     */
    public List<String> getCheckPointFileList();
    /**
     * Given part of a checkpoint file name or the whole file name,
     * this method will attempt to find the most immediately previous
     * check point and return its name.
     * 
     * @param checkPointFileName    contains the date portion of the checkpoint
     *                              file name.
     * @return  the most recent checkpoint file name previous to the specified
     *          file name
     */
    public String getPreviousCheckPoint(String checkPointFileName);
    /**
     * Returns the underlying {@link Kryo} impl.
     * @return the underlying {@link Kryo} impl.
     */
    public Kryo getKryo();   
    /**
     * Writes the specified object out to the configured location on disk.
     * @param instance  the instance to serialize.
     * @return
     */
    public byte[] checkPoint(T instance);
    /**
     * Returns the bytes of the last serialized object or null if there was a problem.
     * @return  the bytes of the last serialized object.
     */
    public byte[] getLastBytes();
    /**
     * Returns the {@link SerialConfig} in use currently.
     * @return      the {@link SerialConfig} in use currently.
     */
    public SerialConfig getConfig();
}
