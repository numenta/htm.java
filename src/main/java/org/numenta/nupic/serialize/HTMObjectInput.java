package org.numenta.nupic.serialize;

import java.io.IOException;
import java.io.InputStream;

import org.numenta.nupic.Persistable;
import org.nustaq.serialization.FSTObjectInput;

public class HTMObjectInput extends FSTObjectInput {
    public HTMObjectInput(InputStream in) throws IOException {
        super(in);
    }
    
    @SuppressWarnings("rawtypes")
    public Object readObject(Class...classes) throws Exception {
        try {
            Object obj = super.readObject(classes);
            
            if(obj instanceof Persistable) {
                ((Persistable) obj).postDeSerialize();
            }
            return obj;
        }catch(Exception e) {
            throw new IOException(e);
        }
    }
}