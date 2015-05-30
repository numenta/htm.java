package org.numenta.nupic.datagen;

import org.junit.Ignore;

import rx.Observer;


public class NetworkInputKitTest {

    @Ignore
    public void test() {
        String[] entries = { 
            "timestamp", "consumption",
            "datetime", "float",
            "B",
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,34.0",
            "7/2/10 2:00,40.4",
        };
        
        final NetworkInputKit kit = new NetworkInputKit();
        
        Thread test = null;
        (test = new Thread() {
            public void run() {
                kit.observe().subscribe(new Observer<String>() {
                    @Override public void onCompleted() {}
                    @Override public void onError(Throwable e) { e.printStackTrace(); }
                    @Override public void onNext(String output) {
                        System.out.println(output);
                    }
                });
            }
        }).start();
       
        for(String s : entries) {
            kit.offer(s);
        }
        
        try {
            test.join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
    }

}
