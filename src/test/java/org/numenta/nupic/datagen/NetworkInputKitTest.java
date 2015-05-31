package org.numenta.nupic.datagen;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import rx.Observer;
import rx.subjects.ReplaySubject;


public class NetworkInputKitTest {

    @Test
    public void testHeaderAndDelayedEntry() {
        ReplaySubject<String> manual = NetworkKit.builder()
            .addHeader("timestamp,consumption")
            .addHeader("datetime,float")
            .addHeader("B")
            .buildPublisher();
        
        final List<String> collected = new ArrayList<>();
        manual.subscribe(new Observer<String>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(String output) {
                collected.add(output);
            }
        });
        
        assertEquals(3, collected.size());
        
        String[] entries = { 
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,34.0",
            "7/2/10 2:00,40.4",
            "7/2/10 3:00,123.4",
        };
        
        for(String s : entries) {
            manual.onNext(s);
        }
        
        assertEquals(7, collected.size());
    }

}
