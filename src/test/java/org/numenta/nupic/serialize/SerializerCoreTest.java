package org.numenta.nupic.serialize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.ManualInput;
import org.numenta.nupic.network.Persistence;


@SuppressWarnings("serial")
public class SerializerCoreTest implements Serializable {

    @Test
    public void testGetSerializer() {
        SerializerCore serializer = Persistence.get().serializer();
        assertNotNull(serializer);
        
        SerializerCore  serializer2 = Persistence.get().serializer();
        assertTrue(serializer == serializer2);
    }

    /**
     * Test HTM wrapper can be created and passes through to underlying implementation.
     */
    @Test
    public void testGetObjectInput() {
        try {
            HTMObjectInput input = Persistence.get().serializer().getObjectInput(new InputStream() {
                @Override public int read() throws IOException {
                    return 0;
                }
            });
            
            assertNotNull(input);
            assertEquals(0, input.read());
        } catch(IOException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    /**
     * Test HTM wrapper can be created and passes through to underlying implementation.
     */
    @Test
    public void testGetObjectOutput() {
        try {
            HTMObjectOutput output = Persistence.get().serializer().getObjectOutput(new OutputStream() {
                @Override public void write(int b) throws IOException {
                    
                }
            });
            
            assertNotNull(output);
            output.write(0);
        } catch(IOException e) {
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void testSerializeDeSerialize() {
        final List<Inference> callVerify = new ArrayList<>();
        
        SerializerCore serializer = Persistence.get().serializer();
        
        Inference inf = new ManualInput() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T postDeSerialize(T i) {
                Inference retVal = (Inference)super.postDeSerialize(i);
                assertNotNull(retVal);
                assertTrue(retVal != i); // Ensure Objects not same
                assertTrue(retVal.equals(i)); // However they are still equal!
                callVerify.add(retVal);
                assertTrue(callVerify.size() == 1);
                
                return (T)retVal;
            }
        };
        
        byte[] bytes = serializer.serialize(inf);
        assertNotNull(bytes);
        
        Inference serializedInf = serializer.deSerialize(bytes);
        assertNotNull(serializedInf);
    }
    
    @Test
    public void testRegisterClass() {
        SerializerCore serializer = Persistence.get().serializer();
        int val = serializer.getSerialScheme().getClassRegistry().getIdFromClazz(SerializerCoreTest.class);
        assertEquals(Integer.MIN_VALUE, val);
        
        serializer.registerClass(SerializerCoreTest.class);
        int val2 = serializer.getSerialScheme().getClassRegistry().getIdFromClazz(SerializerCoreTest.class);
        assertNotEquals(Integer.MIN_VALUE, val2);
    }
}
