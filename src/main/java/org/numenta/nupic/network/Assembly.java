package org.numenta.nupic.network;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.network.Network.Mode;


public abstract class Assembly {
    
    private Parameters params;
    
    
    Assembly(Parameters p) {
        this.params = p;
    }
    
    /**
     * Network    
     * @param p
     * @param m
     * @return
     */
    public static Network create(Parameters p, Mode m) {
        if(m == null) {
            throw new IllegalArgumentException(
                "Mode cannot be null and must be one of: { MANUAL, AUTO, REACTIVE }");
        }
        
        Network retVal = null;
        
        switch(m) {
            case MANUAL: {
                break;
            }
            case AUTO: {
                break;
            }
            case REACTIVE: {
                break;
            }
        }
        
        return retVal;
    }
    
    

}
