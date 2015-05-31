package org.numenta.nupic.datagen;

import org.numenta.nupic.network.Network;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.ReplaySubject;



public class NetworkKit {
    private static final int HEADER_SIZE = 3;
    
    public static class Builder<T> {
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
        public ReplaySubject<String> buildPublisher() {
            ReplaySubject<String> subject = ReplaySubject.create();
            for(int i = 0;i < HEADER_SIZE;i++) {
                if(lines[i] == null) {
                    throw new IllegalStateException("Header not properly formed (must contain 3 lines) see Header.java");
                }
                subject.onNext(lines[i]);
            }
            return subject;
        }
    }
    
    /**
     * Returns a builder that is capable of returning a configured {@link PublishSubject} 
     * (publish-able) {@link Observable}
     * @return
     */
    public static Builder<PublishSubject<String>> builder() {
        return new Builder<>();
    }
}
