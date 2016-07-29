package org.numenta.nupic.network;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.observers.TestObserver;
import rx.subjects.PublishSubject;

/**
 * Indicates the difference in {@link AssertionFailure} handling between
 * {@link Observable}s which are directly subscribed to, and {@code Observable}s
 * which are indirectly subscribed to.
 * 
 * HTM.Java's Network API uses an "indirect" method of emitting events allowing
 * more flexibility in management of event dispatch however it causes a problem
 * within JUnit tests where assertion failures don't actually get dispatched to JUnit
 * so that the test will reliably fail if the conditions of the test aren't met.
 * 
 * This test class illustrates the differences and provides an example of a solution
 * which allows exceptions to be gathered at the site of assertion failure and later
 * propagated to JUnit so that it can fail the test if the test actually has a 
 * failure condition.
 * 
 * @author cogmission
 * @see ObservableTestBase
 */
public class ObservableTestBaseTest extends ObservableTestBase {
    
    /**
     * This test shows that failures arising in {@link Observer#onNext(Object)} can 
     * indeed be caught by JUnit - providing the propagating infrastructure reports
     * the failure directly to the subscribed {@link Observable}. 
     * 
     * HTM.Java's {@link Layer} class however, has an internal {@link PublishSubject}
     * which dispatches all Layer computations and is a level of indirection between
     * the Observable originally subscribed to, and the Observable (PublishSubject)
     * used to dispatch (emit) the Layer events.
     */
    @Test
    public void testDirectObservableSubscriberCanCatchFailedAssertions() {
        Observable<Inference> observable = Observable.create(new Observable.OnSubscribe<Inference>() {
            @Override public void call(Subscriber<? super Inference> subscriber) {
                ManualInput inf = new ManualInput();
                inf.anomalyScore(1.0);
                
                subscriber.onNext(inf);
            }
        });
        
        Observer<Inference> observer = new Observer<Inference>() {
            @Override public void onCompleted() {}
            
            // Here fail works and forces JUnit to pick up the failure
            @Override public void onError(Throwable e) { fail(); }  // <--- PROPAGATE FAILURE TO JUnit
            
            @Override public void onNext(Inference inf) {
                // The inference's anomaly score = 1.0 so this should force failure.
                assertTrue(inf.getAnomalyScore() == 0.0); 
            }
        };
        
        try {
            observable.subscribe(observer);
            
            // This test would fail if this point was reached
            // (but it isn't reached because the above failed assertion is correctly
            // caught by JUnit - as it should be). Instead the above throws an 
            // exception.
            fail();   
            
        }catch(Exception e) {
            
        }
    }
    
    /**
     * Demonstrates that the indirection of the internal PublishSubject within
     * the Network's Layer class is what causes failed assertions to not be
     * recognized by JUnit (i.e. they get swallowed). 
     * 
     * This test passes even though there are errors.
     */
    @Test
    public void testCheckObservable_Incorrectly_Passes() {
        FauxNetwork network = new FauxNetwork();
        
        TestObserver<Inference> observer = new TestObserver<Inference>() {
            @Override
            public void onNext(Inference i) {
                assertTrue(i.getAnomalyScore() == 0.0);
            }
        };
        
        network.subscribe(observer);
        
        ManualInput inf = new ManualInput();
        inf.anomalyScore(1.0);
        
        network.compute(inf);
        
        // Test that there are errors even though the test passes.
        assertTrue(hasErrors(observer));
    }
    
    /**
     * Demonstrates that the indirection of the internal PublishSubject within
     * the Network's Layer class is what causes failed assertions to not be
     * recognized by JUnit (i.e. they get swallowed). 
     * 
     * This test passes even though there are errors.
     */
    @Test
    public void testCheckObservable_Correctly_Fails() {
        FauxNetwork network = new FauxNetwork();
        
        TestObserver<Inference> observer = new TestObserver<Inference>() {
            @Override
            public void onNext(Inference i) {
                assertTrue(i.getAnomalyScore() == 0.0); // Force error: anomalyScore == 1.0
            }
        };
        
        network.subscribe(observer);
        
        ManualInput inf = new ManualInput();
        inf.anomalyScore(1.0);
        
        network.compute(inf);
        
        // Test that there are errors even thought the test passes.
        assertTrue(hasErrors(observer));
        
        try {
            checkObserver(observer);
            // Should not reach this point. We want the checkObserver()
            // method above to throw an exception which will indicate the
            // test failure - though the test failing is what makes the 
            // actual (negative) test pass.
            fail(); 
        }catch(Exception e) {
            // Test passes when this point is reached.
            assertTrue(e.getCause() instanceof AssertionError);
        }
    }


    /**
     * Mimics the internal dispatching of the Layer class of the Network package
     * which uses a PublishSubject internally for central management of subscriptions
     * and whose indirection causes the asserts within {@link Observer#onNext(Object)}
     * to pass the tests when they actually shouldn't.
     */
    class FauxNetwork {
        List<Observer<Inference>> observers = new ArrayList<>();
        
        PublishSubject<Inference> internalDispatch = PublishSubject.create();
        Observable<Inference> clientObservable;
        
        public FauxNetwork() {
            internalDispatch.subscribe(new Observer<Inference>() {
                @Override public void onCompleted() {}
                @Override public void onError(Throwable e) { e.printStackTrace(); }
                @Override public void onNext(Inference i) {
                    for(Observer<Inference> o : observers) {
                        o.onNext(i);
                    }
                }
            });
            
            clientObservable = Observable.create(new Observable.OnSubscribe<Inference>() {
                @SuppressWarnings("unchecked")
                @Override public void call(Subscriber<? super Inference> t) {
                    observers.add((Observer<Inference>)t);
                }
            });
        }
        
        public void subscribe(Observer<Inference> subscriber) {
            clientObservable.subscribe(subscriber);
        }
        
        public void compute(Inference inference) {
            internalDispatch.onNext(inference);
        }
    }
}
