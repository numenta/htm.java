package org.numenta.nupic.util;

import java.io.IOException;

import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.NetworkSerializer;
import org.numenta.nupic.network.SerialConfig;
import org.numenta.nupic.network.NetworkSerializer.Scheme;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTClazzInfo.FSTFieldInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

public class TestSerializeContainerSerializer extends FSTBasicObjectSerializer {
    SerialConfig config = new SerialConfig("testObjectSerializers", Scheme.FST);
    NetworkSerializer<TestSerializeContainer> serializer = Network.serializer(config, true, false);
    
    
    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        System.out.println("TestSerializeContainerSerializer Reached writeObject");
        byte[] bytes = serializer.serialize((TestSerializeContainer)toWrite, true);
        out.writeObjectInternal(bytes, null, (Class[])null);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
        System.out.println("TestSerializeContainerSerializer Reached instantiate()");
        byte[] bytes = (byte[])in.readObjectInternal(objectClass);
        TestSerializeContainer tsc = serializer.deSerialize(objectClass, bytes);
        
        return tsc;
    }

}
