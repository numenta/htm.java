package org.numenta.nupic.network;

import java.io.File;

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
     * to serialize the specified Object instance. If the scheme was previously set to {@link Scheme#FST}, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance  the object instance to serialize
     */
    public byte[] serialize(T instance);
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to serialize the specified Object instance. If the scheme was previously set to {@link Scheme#FST}, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance      the object instance to serialize
     * @param bytesOnly     flag indicating whether to persist to disk or permanent storage.
     */
    public byte[] serialize(T instance, boolean bytesOnly);
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to deserialize the specified Class type. If the scheme was previously set to {@link Scheme#FST}, a byte
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
     * Returns the underlying {@link Kryo} impl.
     * @return the underlying {@link Kryo} impl.
     */
    public Kryo getKryo();    
}
