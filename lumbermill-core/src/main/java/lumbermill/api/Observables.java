/*
 * Copyright 2016 Sony Mobile Communications, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package lumbermill.api;

import lumbermill.internal.ExponentialTimer;
import lumbermill.internal.FixedTimer;
import lumbermill.internal.LinearTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

/**
 * Utility class for working with Observables
 */
public class Observables {

    private static final Logger LOGGER = LoggerFactory.getLogger(Observables.class);

    /**
     * Prepare values
     * @param valueToWrap could be any value
     * @param <T> could be any type
     * @return An ObservableBuilder to operate on
     */
    public static <T> ObservableBuilder just(T... valueToWrap) {
        return new ObservableBuilder(valueToWrap);
    }


    public static Timer.Factory linearTimer(int delayMs) {
        return new LinearTimer(delayMs);
    }

    public static Timer.Factory fixedTimer(int delayInMs) {
        return new FixedTimer(delayInMs);
    }

    public static Timer.Factory exponentialTimer(int seedDelayMs) {
        return new ExponentialTimer(seedDelayMs);
    }


    public static class ObservableBuilder<T> {

        private final T[] value;

        private ObservableBuilder(T... value) {
            this.value = value;
        }

        public Observable<T> withDelay(Observable<Long> timer) {
            return timer.flatMap(aLong -> Observable.from(value));
        }

        public Observable<T> withDelay(Timer timer) {
            return timer.next().flatMap(aLong -> Observable.from(value));
        }
    }
}
