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
package lumbermill.internal;

import lumbermill.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A timer with exponentially increasing value
 */
public class ExponentialTimer implements Timer, Timer.Factory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExponentialTimer.class);

    private AtomicInteger attempt = new AtomicInteger(1);

    private final float seed;

    public ExponentialTimer(int seedMs) {
        if (seedMs < 1000) {
            LOGGER.warn("Exponential timer is currently using a minimum delay of 1 sec. " +
                    "Provided seed MUST be more than 1000 ms but was {}.", seedMs);
        }
        this.seed = (float)Math.max(1000, seedMs) / (float)1000;
    }

    private ExponentialTimer(float seed) {
        this.seed = seed;
    }

    @Override
    public Observable<Long> next() {
        long nextRetryDelay = (long) (Math.pow(seed, attempt.get()) * 1000);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Delaying operation in {} ms (seed={}, attempt={})", nextRetryDelay, seed, attempt.get());
        }
        attempt.incrementAndGet();
        return Observable.timer(nextRetryDelay,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public int attempt() {
        return attempt.get();
    }

    @Override
    public Timer create() {
        return new ExponentialTimer(seed);
    }
}
