package org.numenta.nupic.network;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.numenta.nupic.util.TestSerializeContainer;
import org.numenta.nupic.util.TestSerializeContainerSerializer;
import org.nustaq.serialization.FSTConfiguration;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;


class NetworkSerializerImpl<T> extends Serializer<T> implements NetworkSerializer<T> {
    /** Use of Fast Serialize https://github.com/RuedigerMoeller/fast-serialization */
    private final FSTConfiguration fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
    
    /** Kryo serializer */
    private Kryo kryo;
    
    /** Indicates the underlying serialization scheme to use see {@link NetworkSerializer.Scheme} */
    private SerialConfig config;
    private Scheme scheme;
    
    private File serialPath;
    
    private Output kryoOutput;
    private Input kryoInput;
    
    
    
    /**
     * Constructs a new {@code NetworkSerializerImpl} which will use the specified
     * underlying serialization scheme.
     * 
     * @param config      the configuration to use
     */
    NetworkSerializerImpl(SerialConfig config, boolean autoRegisterSerializers) {
        this.config = config;
        this.scheme = config.getScheme();
        
        try {
            ensurePathExists(config);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        if(config.getScheme() == Scheme.KRYO) {
            this.kryo = createKryo();
        }
        
        if(autoRegisterSerializers) {
            registerSerializers();
        }
    }
    
    public void registerSerializers() {
        //fastSerialConfig.registerClass(DateEncoder.class);
        //fastSerialConfig.registerSerializer(DateEncoder.class, new DateEncoderSerializer(), false);
        fastSerialConfig.registerSerializer(TestSerializeContainer.class, new TestSerializeContainerSerializer(), true);
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
     * @return
     */
    public T read(Kryo kryo, Input in, Class<T> klass) {
        return deSerialize(klass, null);
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
     * @return
     */
    public void write(Kryo kryo, Output out, T type) {
        serialize(type);
    }
    
    /**
     * Delegates to the underlying serialization scheme (specified by {@link Network#serializer(Scheme, boolean)}
     * to serialize the specified Object instance. If the scheme was previously set to {@link Scheme#FST}, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance  the object instance to serialize
     */
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
     * to serialize the specified Object instance. If the scheme was previously set to {@link Scheme#FST}, a byte
     * array is returned - though both schemes serialize to an underlying file.
     * 
     * @param instance      the object instance to serialize
     * @param bytesOnly     flag indicating whether to persist to disk or permanent storage.
     */
    public byte[] serialize(T instance, boolean bytesOnly) {
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                byte[] bytes = fastSerialConfig.asByteArray(instance);
                if(!bytesOnly) {
                    try {
                        Files.write(serialPath.toPath(), bytes, config.getOpenOptions());
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return bytes;
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
     * @param bytes     (Optional - may be null) for use with FST underlying scheme.
     * @return  the deserialized object
     */
    @SuppressWarnings("unchecked")
    public T deSerialize(Class<T> type, byte[] bytes) {
        switch(scheme) {
            case KRYO: // Fall through to FST Handler
            case FST: {
                if(bytes == null) {
                    try {
                        bytes = Files.readAllBytes(serialPath.toPath());
                    } catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return (T)fastSerialConfig.asObject(bytes);
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * Returns the File object that the serialization scheme writes to.
     * @return
     */
    public File getSerializedFile() {
        return serialPath;
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
        
        serialPath = new File(customDir.getAbsolutePath() + File.separator +  config.getFileName());
        if(!serialPath.exists()) {
            serialPath.createNewFile();
        }
        
        return true;
    }
    
    /**
     * Returns a {@link Kryo} implementation capable of delegating to FST :-P
     * @return
     */
    private Kryo createKryo() {
        return new KryoReflectionFactorySupport() {
            private NetworkSerializer<?> ser;
            private SerialConfig conf;
            
            {
                conf = new SerialConfig(
                    config.getFileName(), Scheme.FST, 
                        config.getRegistry(), config.getOpenOptions());
                ser = Network.serializer(conf, true, true);
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
