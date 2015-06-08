/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import org.numenta.nupic.network.Layer;
import org.numenta.nupic.network.Network;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;


/**
 * Provides a clean way to create an {@link rx.Observable} with which one can input CSV
 * Strings. It ensures that the underlying Observable's stream is set up properly with
 * a header with exactly 3 lines. These header lines describe the input in such a way 
 * as to specify input names and types together with control data for automatic Stream
 * consumption as designed by Numenta's input file format.
 * 
 * <b>NOTE:</b> The {@link Publisher.Builder#addHeader(String)} method must be called
 * before adding the publisher to the {@Link Layer} (i.e. {@link Sensor}).
 * 
 * Typical usage is as follows:
 * <pre>
 * <h1>In the case of manual input</h1>
 * Publisher manualPublisher = Publisher.builder()
 *     .addHeader("timestamp,consumption")
 *     .addHeader("datetime,float")
 *     .addHeader("B")
 *     .build();
 * 
 * ...then add the object to a {@link SensorParams}, and {@link Sensor}
 * 
 * Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
 *     ObservableSensor::create, 
 *         SensorParams.create(
 *             Keys::obs, new Object[] { "your name", manualPublisher }));
 *             
 * ...you can then add the "sensor" to a {@link Layer}
 * 
 * Layer<int[]> l = new Layer<>(n)
 *     .addSensor(sensor);
 *     
 * ...then manually input comma separated strings as such:
 * 
 * String[] entries = { 
 *     "7/2/10 0:00,21.2",
 *     "7/2/10 1:00,34.0",
 *     "7/2/10 2:00,40.4",
 *     "7/2/10 3:00,123.4",
 * };
 * 
 * manual.onNext(entries[0]);
 * manual.onNext(entries[1]);
 * manual.onNext(entries[2]);
 * manual.onNext(entries[3]);
 * </pre>
 * 
 * @author David Ray
 *
 */
public class Publisher {
    private static final int HEADER_SIZE = 3;
    
    private ReplaySubject<String> subject;
    
    public static class Builder<T> {
        private ReplaySubject<String> subject;
        
        String[] lines = new String[3];
        int cursor = 0;
        /**
         * Adds a header line which in the case of a multi column input 
         * is a comma separated string.
         * 
         * @param s
         * @return
         */
        @SuppressWarnings("unchecked")
        public Builder<PublishSubject<String>> addHeader(String s) {
            lines[cursor] = s;
            ++cursor;
            return (Builder<PublishSubject<String>>)this;
        }
        
        /**
         * Builds and validates the structure of the expected header then
         * returns an {@link Observable} that can be used to submit info to the
         * {@link Network}
         * @return
         */
        public Publisher build() {
            subject = ReplaySubject.createWithSize(3);
            for(int i = 0;i < HEADER_SIZE;i++) {
                if(lines[i] == null) {
                    throw new IllegalStateException("Header not properly formed (must contain 3 lines) see Header.java");
                }
                subject.onNext(lines[i]);
            }
            
            Publisher p = new Publisher();
            p.subject = subject;
            
            return p;
        }
    }
    
    /**
     * Returns a builder that is capable of returning a configured {@link PublishSubject} 
     * (publish-able) {@link Observable}
     * 
     * @return
     */
    public static Builder<PublishSubject<String>> builder() {
        return new Builder<>();
    }
    
    /**
     * Provides the Observer with a new item to observe.
     * <p>
     * The {@link Observable} may call this method 0 or more times.
     * <p>
     * The {@code Observable} will not call this method again after it calls either {@link #onCompleted} or
     * {@link #onError}.
     * 
     * @param t
     *          the item emitted by the Observable
     */
    public void onNext(String input) {
        subject.onNext(input);
    }
    
    /**
     * Notifies the Observer that the {@link Observable} has finished sending push-based notifications.
     * <p>
     * The {@link Observable} will not call this method if it calls {@link #onError}.
     */
    public void onComplete() {
        subject.onCompleted();
    }
    
    /**
     * Notifies the Observer that the {@link Observable} has experienced an error condition.
     * <p>
     * If the {@link Observable} calls this method, it will not thereafter call {@link #onNext} or
     * {@link #onCompleted}.
     * 
     * @param e     the exception encountered by the Observable
     */
    public void onError(Throwable e) {
        subject.onError(e);
    }
    
    /**
     * Subscribes to an Observable and provides an Observer that implements functions to handle the items the
     * Observable emits and any error or completion notification it issues.
     * <dl>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>{@code subscribe} does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     *
     * @param observer
     *             the Observer that will handle emissions and notifications from the Observable
     * @return a {@link Subscription} reference with which the {@link Observer} can stop receiving items before
     *         the Observable has completed
     * @see <a href="http://reactivex.io/documentation/operators/subscribe.html">ReactiveX operators documentation: Subscribe</a>
     */
    public Subscription subscribe(Observer<String> observer) {
        return subject.subscribe(observer);
    }
    
    /**
     * Called within package to access this {@link Publisher}'s wrapped {@link Observable}
     * @return
     */
    Observable<String> observable() {
        return subject;
    }
}
