package org.numenta.nupic.serialize;

import java.io.IOException;
import java.io.InputStream;

import org.numenta.nupic.model.Persistable;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;

public class HTMObjectInput extends FSTObjectInput {
    public HTMObjectInput(InputStream in, FSTConfiguration config) throws IOException {
        super(in, config);
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