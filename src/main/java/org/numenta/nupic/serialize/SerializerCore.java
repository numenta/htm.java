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
package org.numenta.nupic.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.network.PersistenceAPI;
import org.nustaq.serialization.FSTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * {@link PeristenceAPI} underbelly which uses the FST Fast Serialization library to
 * serialize various HTM.java object.
 * 
 * @author cogmission
 * @see PersistenceAPI
 * @see SerialConfig
 */
public class SerializerCore implements Persistable {
    private static final long serialVersionUID = 1L;
    
    protected static final Logger LOGGER = LoggerFactory.getLogger(SerializerCore.class);
    
    private Class<?>[] classes;
    
    /** Use of Fast Serialize https://github.com/RuedigerMoeller/fast-serialization */
    private transient FSTConfiguration fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
    
    
    /**
     * Constructs a new {@code SerializerCore}, registering the
     * specified classes to avoid extraneous writing of filenames to 
     * the serialized stream.
     * 
     * @param classes       the class array of registered classes
     */ 
    public SerializerCore(Class<?>...classes) {
        this.classes = classes;
        initFST();
    }
    
    /**
     * Initializes the delegate serializer
     */
    private void initFST() {
        fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
        if(classes != null) {
            fastSerialConfig.registerClass(classes);
        }
    }
    
    /**
     * Registers the specified classes which obviates the need to write class names
     * to the stream, saving processing time and space.
     * @param c     an array of classes to register
     */
    @SuppressWarnings("rawtypes")
    public void registerClass(Class... c) {
        fastSerialConfig.registerClass(c);
    }
    
    /**
     * Called by whatever serializable infrastructure would want to serialize this
     * {@code SerializerCore}
     * 
     * @param in                        the stream containing this {@code SerializerCore}
     * @throws IOException              as a result of io problems
     * @throws ClassNotFoundException   if classes contained within the stream cannot be resolved
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        initFST();
    }

    /**
     * Returns the specified {@link InputStream} wrapped in an HTM input stream
     * @param is    the InputStream to wrap
     * @return  the FSTObjectInput
     * @throws IOException 
     */
    public HTMObjectInput getObjectInput(InputStream is) throws IOException {
        return new HTMObjectInput(is, fastSerialConfig);
    }
    
    /**
     * Returns the specified {@link OutputStream} wrapped in an HTM output stream
     * @param is    the OutputStream to wrap
     * @return  the HTMObjectOutput
     */
    public <T extends Persistable> HTMObjectOutput getObjectOutput(OutputStream os) {
        return new HTMObjectOutput(os, fastSerialConfig);
    }
    
    /**
     * Serializes the specified {@link Persistable} to a byte array
     * then returns it.
     * @param instance  the instance of Persistable to serialize
     * @return  the byte array
     */
    public <T extends Persistable> byte[] serialize(T instance) {
        byte[] bytes = null;
        try {
            bytes = fastSerialConfig.asByteArray(instance);
        } catch(Exception e) {
            bytes = null;
            throw new RuntimeException(e);
        }
        
        return bytes;
    }
    
    /**
     * Deserializes the specified Class type from the specified byte array
     * 
     * @param bytes     the byte array containing the object to be deserialized
     * @return  the deserialized object of type &lt;T&gt;
     */
    @SuppressWarnings("unchecked")
    public <T extends Persistable> T deSerialize(byte[] bytes) {
        T retVal = (T)fastSerialConfig.asObject(bytes);
        return retVal.postDeSerialize();
    }
    
    /**
     * For testing only!
     * @return
     */
    FSTConfiguration getSerialScheme() {
        return fastSerialConfig;
    }
}
