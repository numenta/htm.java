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
 * Subclasses {@link rx.Observable} to enforce more poignant subscription behavior
 * and method name.
 * 
 * @author cogmission
 *
 * @param <T>   
 */
public interface CheckPointer<T> {
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
