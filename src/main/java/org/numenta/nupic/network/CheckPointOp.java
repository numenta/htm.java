/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network;

import rx.Observer;
import rx.Subscription;

/**
 * <p>
 * Executes check point behavior through the {@link #checkPoint(Observer)} method. The
 * checkPoint() method adds the specified {@link rx.Observer} to the list of those
 * observers notified following a check point operation. This "subscribe" action invokes
 * the underlying check point operation and returns a notification. The notification consists of
 * a byte[] containing the serialized {@link Network}.
 * </p><p>
 * <b>Typical usage is as follows:</b>
 * <pre>
 *  {@link Persistence} p = Persistence.get();
 *  
 *  p.checkPointOp().checkPoint(new Observer<byte[]>() { 
 *      public void onCompleted() {}
 *      public void onError(Throwable e) { e.printStackTrace(); }
 *      public void onNext(byte[] bytes) {
 *          // Do work here, use serialized Network byte[] here if desired...
 *      }
 *  });
 * 
 * Again, by subscribing to this CheckPointOp, the Network knows to check point after completion of 
 * the current compute cycle (it checks the List of Observers to see if it's non-empty).
 * Then after it notifies all current observers, it clears the list prior to the next 
 * following compute cycle. see {@link PAPI} for a more detailed discussion...
 * 
 * @author cogmission
 *
 * @param <T>  the notification return type
 */
@FunctionalInterface
public interface CheckPointOp<T> {
    /**
     * Registers the Observer for a single notification following the checkPoint
     * operation. The user will be notified with the byte[] of the {@link Network}
     * being serialized.
     * 
     * @param t     a {@link rx.Observer}
     * @return  a Subscription object which is meaningless.
     */
    public Subscription checkPoint(Observer<? super T> t);
}
