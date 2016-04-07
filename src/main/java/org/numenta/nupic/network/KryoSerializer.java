package org.numenta.nupic.network;

import java.io.Serializable;

import org.numenta.nupic.Persistable;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Primitive Kryo serializer based on FST.
 *
 * <b>Warning: Do not use!</b>
 * To only be called by a <a href="https://ci.apache.org/projects/flink/flink-docs-master/
 * apis/best_practices.html#register-a-custom-serializer-for-your-flink-program">Flink</a>
 * cluster server.
 *
 * Use instead {@link Network#serializer(SerialConfig, boolean)}
 */
public class KryoSerializer<T> extends Serializer<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Class<?>[] classes;

    /** Use of Fast Serialize https://github.com/RuedigerMoeller/fast-serialization */
    private transient FSTConfiguration fastSerialConfig;

    public KryoSerializer(Class<?>... c) {
        this.classes = c;
        initFST();
    }

    private void initFST() {
        fastSerialConfig = FSTConfiguration.createDefaultConfiguration();
        fastSerialConfig.registerClass(classes);
    }

    /**
     *
     * @param kryo      instance of {@link Kryo} object
     * @param output    a Kryo {@link Output} object
     * @param t         instance to serialize
     */
    @Override
    public void write(Kryo kryo, Output output, T t) {
        try {
            if(t instanceof Persistable) {
                ((Persistable) t).preSerialize();
            }
            
            byte[] bytes = fastSerialConfig.asByteArray(t);
            output.write(bytes);
        }
        catch(Exception e) {
            throw new KryoException(e);
        }
    }

    /**
     *
     * @param kryo      instance of {@link Kryo} object
     * @param input     a Kryo {@link Input}
     * @param aClass    The class of the object to be read in.
     * @return  an instance of type &lt;T&gt;
     */
    @SuppressWarnings("unchecked")
    @Override
    public T read(Kryo kryo, Input input, Class<T> aClass) {
        FSTObjectInput reader = fastSerialConfig.getObjectInput(input);
        try {
            T t = (T) reader.readObject(aClass);

            if(t instanceof Persistable) {
                ((Persistable) t).postDeSerialize();
            }
            return t;
        }
        catch(Exception e) {
            throw new KryoException(e);
        }
    }
}
