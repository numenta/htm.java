package org.numenta.nupic.serialize;

import java.io.IOException;
import java.io.OutputStream;

import org.numenta.nupic.Persistable;
import org.nustaq.serialization.FSTObjectOutput;

public class HTMObjectOutput extends FSTObjectOutput {
    public HTMObjectOutput(OutputStream out) {
        super(out);
    }
    
    @SuppressWarnings("rawtypes")
    public void writeObject(Object t, Class... c) throws IOException {
        if(t instanceof Persistable) {
            ((Persistable) t).preSerialize();
        }
        
        super.writeObject(t, c);
    }
}