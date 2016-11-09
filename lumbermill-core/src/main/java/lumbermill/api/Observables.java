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
import lumbermill.internal.MapWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for working with Observables
 */
public class Observables {

    private static final Logger LOGGER = LoggerFactory.getLogger(Observables.class);

    private final static int DEFAULT_DELAY_MS = 1000;
    private final static int DEFAULT_FIXED_DELAY_ATTEMPTS = 3;
    private final static int DEFAULT_EXPONENTIAL_SEED     = 2000;

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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating a new LinearTimer with delayMs {}", delayMs);
        }
        return new LinearTimer(delayMs);
    }

    public static Timer.Factory fixedTimer(int delayMs) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating a new FixedTimer with delayMs {}", delayMs);
        }
        return new FixedTimer(delayMs);
    }

    public static Timer.Factory exponentialTimer(int seedDelayMs) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating a new ExponentialTimer with seed delayMs {}", seedDelayMs);
        }
        return new ExponentialTimer(seedDelayMs);
    }

    /**
     * Convenience method for creating a Timer.Factory from configuration
     * Not sure it belongs here...
     */
    public static Timer.Factory timer(MapWrap retry) {
        MapWrap mapWrap = retry.assertExists("policy");
        String policy = mapWrap.get("policy");

        if (policy.equals("linear")) {
            return linearTimer(mapWrap.get("delayMs", DEFAULT_DELAY_MS));
        } else if (policy.equals("fixed")) {
            return fixedTimer(mapWrap.get("delayMs", DEFAULT_DELAY_MS));
        } else if (policy.equals("exponential")) {
            return exponentialTimer(mapWrap.get("delayMs", DEFAULT_EXPONENTIAL_SEED));
        }
        throw new IllegalStateException("Expected one of [linear, fixed, exponential] but got " + policy);
    }



    public static <T>Observable<T> observe(CompletableFuture<T> future) {
        return Observable.create(subscriber -> {
            future.whenComplete((value, exception) -> {
                if (exception != null) {
                    subscriber.onError(exception);
                } else {
                    subscriber.onNext(value);
                    subscriber.onCompleted();
                }
            });
        });
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
