package org.numenta.nupic.network;

import rx.observers.TestObserver;

/**
 * Base class for tests which use the Observable framework to test
 * their methods. Contains useful methods to 
 * @author cogmission
 *
 */
public class ObservableTestBase {
    
    protected <T> void checkObserver(TestObserver<T> obs) {
        if(obs.getOnErrorEvents().size() > 0) {
            Throwable e = (Throwable) obs.getOnErrorEvents().get(0);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    protected <T> boolean hasErrors(TestObserver<T> obs) {
        return !obs.getOnErrorEvents().isEmpty();
    }
    
    protected <T> boolean hasCompletions(TestObserver<T> obs) {
        return !obs.getOnCompletedEvents().isEmpty();
    }
}
