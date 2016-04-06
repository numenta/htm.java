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
import java.io.IOException;
import java.util.List;

import org.numenta.nupic.Persistable;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * <p>
 * Low-level serialization utility methods using the <a href="https://github.com/RuedigerMoeller/fast-serialization">
 * FST</a> framework, while simultaneously implementing the Kryo {@link Serializer} interface so that {@link Network}
 * serializations can take place within frameworks like <a href="https://github.com/EsotericSoftware/kryo">Kryo</a>
 * and <a href="https://flink.apache.org">Flink</a>.
 * </p>
 
 * @author cogmission
 *
 * @param <T>   the "type" to be serialized (most often {@link Network} or {@link Inference})
 */
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
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to deserialize the specified Class type using the specified file name (minus path for usual case,
     * though you may specify a "sub" path by prepending a path to the filename).
     * 
     * @param type          the subclass of {@link Persistable} to be deserialized.
     * @param fileName      the name of the file containing the serialized Persistable to be returned.
     * @return  the serialized Persistable
     * @throws IOException      if the file doesn't exist
     */
    public T deSerializeFromFile(Class<T> type, String fileName) throws IOException;
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
