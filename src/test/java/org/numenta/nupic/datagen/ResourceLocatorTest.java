package org.numenta.nupic.datagen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URI;

import org.junit.Test;


public class ResourceLocatorTest {

    @Test
    public void testURICreation() {
        try {
            ResourceLocator.uri(".");
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals(java.net.MalformedURLException.class, e.getCause().getClass());
        }
        
        try {
            URI uri = ResourceLocator.uri("file:///.");
            assertNotNull(uri);
            
            assertFalse(uri.isOpaque());
        }catch(Exception e) {
            fail();
        }
    }

}
