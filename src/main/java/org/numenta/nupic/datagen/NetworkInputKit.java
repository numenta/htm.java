package org.numenta.nupic.datagen;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import rx.Observable;


public class NetworkInputKit implements Iterator<String>, Iterable<String> {
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    
    private String next;
    @Override
    public boolean hasNext() {
        if (next != null) {
            return true;
        } else {
            try {
                next = queue.take();
                return (next != null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String next() {
        if (next != null || hasNext()) {
            String line = next;
            next = null;
            return line;
        } else {
            throw new NoSuchElementException();
        }
    }
    
    public void offer(String entry) {
        queue.offer(entry);
    }
    
    public Stream<String> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
            this, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }
    
    @SuppressWarnings("unchecked")
    public Observable<String> observe() {
        Observable<?> o = Observable.from(this);
        return (Observable<String>)o;
    }
}
