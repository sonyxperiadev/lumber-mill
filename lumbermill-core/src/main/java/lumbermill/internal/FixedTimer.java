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
 * A timer with a fixed time, returns the same value each time
 */
public class FixedTimer implements Timer, Timer.Factory {

    private static final Logger LOGGER = LoggerFactory.getLogger(FixedTimer.class);

    private final AtomicInteger attempt = new AtomicInteger(1);

    private final long delayInMs;

    public FixedTimer(long delayInMs) {
        this.delayInMs = delayInMs;
    }

    @Override
    public Observable<Long> next() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Delaying operation in {} ms, attempt={})", delayInMs, attempt.get());
        }
        attempt.incrementAndGet();
        return Observable.timer(delayInMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public int attempt() {
        return attempt.get();
    }

    @Override
    public Timer create() {
        return new FixedTimer(delayInMs);
    }
}
