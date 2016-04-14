package org.numenta.nupic.encoders;

import java.io.IOException;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTClazzInfo.FSTFieldInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;


public class DateEncoderSerializer extends FSTBasicObjectSerializer {
    
    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        // TODO Auto-generated method stub
        System.out.println("DateEncoderSerializer Reached writeObject");
        out.writeObject(toWrite);
//        out.flush();
//        out.close();
    }
    
    @Override
    public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
        System.out.println("DateEncoderSerializer Reached readObject() with: " + toRead);
        String customFormatPattern = ((DateEncoder)toRead).getCustomFormatPattern();
        if(customFormatPattern != null) {
            // Do my work here if things go ok
        }
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
        System.out.println("DateEncoderSerializer Reached instantiate()");
//        DateEncoder de = DateEncoder.builder().build();
//        in.defaultReadObject(referencee,serializationInfo,de);
//        Object obj = in.readObject();
//        Object res = in.readObject();
        
        return null;
    }

}
