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

import groovy.lang.Tuple2;
import lumbermill.RetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Notification;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


public class RetryStrategyImpl implements RetryStrategy {

    //TODO - User observable.filter() with exceptionMatch() for include/exclude a specific exception

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryStrategyImpl.class);

    private final static int DEFAULT_FIXED_DELAY_MS       = 1000;
    private final static int DEFAULT_FIXED_DELAY_ATTEMPTS = 3;
    private final static float DEFAULT_EXPONENTIAL_SEED     = 2.0f;

    private final List<Class<? extends Throwable>> retryOn = new ArrayList<>();

    public RetryStrategyImpl() {}

    public RetryStrategyImpl retryOn(List<Class<? extends Throwable>> retryOn) {
        this.retryOn.addAll(retryOn);
        return this;
    }

    public RetryStrategyImpl retryOn(Class<? extends Throwable> retryOn) {
        this.retryOn.add(retryOn);
        return this;
    }

    /**
     * Convenience method for using default arguments
     */
    @Override
    public Func1<Observable<? extends Throwable>, Observable<?>> withFixedDelay() {
        return withFixedDelay(Collections.EMPTY_MAP);
    }

    @Override
    public Func1<Observable<? extends Throwable>, Observable<?>> withLinearDelay() {
        return withLinearDelay(Collections.EMPTY_MAP);
    }


    @Override
    public Func1<Observable<? extends Throwable>, Observable<?>> withLinearDelay(Map map) {
        MapWrap arguments = MapWrap.of(map);
        int attempts   = arguments.get("attempts", DEFAULT_FIXED_DELAY_ATTEMPTS);
        int delayInMs  = arguments.get("delay",    DEFAULT_FIXED_DELAY_MS);

        return errorNotification -> errorNotification
                .doOnEach(throwable -> printException(throwable))
                .zipWith(Observable.range(1, attempts), RetryStrategyImpl::create)
                .flatMap(attempt ->
                        attempt.getSecond() == attempts || !exceptionMatch(attempt.getFirst()) ?
                                Observable.error(attempt.getFirst()) :
                                linearDelayTimer(delayInMs, attempt.getSecond()));
    }

    private void printException(Notification<?> notification) {
        Optional<Throwable> throwable = extractThrowable(notification);
        if (throwable.isPresent()) {
            LOGGER.info("Got exception of type {} and with message {}",
                    throwable.get().getClass().getSimpleName(),
                    throwable.get().getMessage());
        }
    }


    private Optional<Throwable> extractThrowable(Notification<?> notification) {
        if (notification.getThrowable() != null) {
            return Optional.of(notification.getThrowable());
        }
        if (notification.getValue() != null) {
            return Optional.of((Throwable)notification.getValue());
        }

        return Optional.empty();
    }

    @Override
    public Func1<Observable<? extends Throwable>, Observable<?>> withFixedDelay(Map map) {
        MapWrap arguments = MapWrap.of(map);
        int attempts   = arguments.get("attempts", DEFAULT_FIXED_DELAY_ATTEMPTS);
        int delayInMs  = arguments.get("delay", DEFAULT_FIXED_DELAY_MS);

        return errorNotification -> errorNotification
                .doOnEach(throwable -> printException(throwable))
                .zipWith(Observable.range(1, attempts), RetryStrategyImpl::create)
                .flatMap(attempt ->
                    attempt.getSecond() == attempts || !exceptionMatch(attempt.getFirst()) ?
                        Observable.error(attempt.getFirst()) :
                            fixedDelayTimer(delayInMs, attempt.getSecond()));
    }


    @Override
    public Func1<Observable<? extends Throwable>, Observable<?>> withExponentialDelay() {
        return withExponentialDelay(Collections.EMPTY_MAP);
    }


    @Override
    public Func1<Observable<? extends Throwable>, Observable<?>> withExponentialDelay(Map map) {

        MapWrap arguments = MapWrap.of(map);

        int attempts = arguments.get("attempts", DEFAULT_FIXED_DELAY_ATTEMPTS);
        float seed  = arguments.exists("seed") ? arguments.asFloat("seed") : DEFAULT_EXPONENTIAL_SEED;

        return errorNotification -> errorNotification
                .doOnEach(throwable -> printException(throwable))
                .zipWith(Observable.range(1, attempts), RetryStrategyImpl::create)
                .flatMap(attempt ->
                        attempt.getSecond() == attempts || !exceptionMatch(attempt.getFirst()) ?
                                Observable.error(attempt.getFirst()) : exponentialTimer(seed, attempt.getSecond()));
    }


    private static Observable<Long> fixedDelayTimer(int delayInMs, int attempt) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(FixedDelay) Retrying operation in {} ms, attempt={})", delayInMs, attempt);
        }
        return Observable.timer(delayInMs, TimeUnit.MILLISECONDS);
    }

    private static Observable<Long> linearDelayTimer(int delayInMs, int attempt) {
        int delay = delayInMs * attempt;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(LinearDelay) Retrying operation in {} ms (seedDelay={} attempt={})", delay, delayInMs, attempt);
        }
        return Observable.timer(delay, TimeUnit.MILLISECONDS);
    }

    private static Observable<Long> exponentialTimer(float seed, int attempt) {
        long nextRetryDelay = (long) (Math.pow(seed, attempt) * 1000);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("(ExponentialDelay) Retrying operation in {} ms (seed={}, attempt={})", nextRetryDelay, seed, attempt);
        }
        return Observable.timer(nextRetryDelay,
                TimeUnit.MILLISECONDS);
    }

    private static Tuple2<Throwable, Integer> create(Throwable t, int attempt) {
        return new Tuple2<>(t, attempt);
    }

    /**
     * Returns true if the specified is a subclass or implementation of any of the specified
     * exception types or if no exception types are specified.
     */
    private boolean exceptionMatch(Throwable ex) {

        if (retryOn.size() == 0) {
            return true;
        }

        return retryOn
                .stream()
                .filter(c -> c.isAssignableFrom(ex.getClass()) )
                .count() > 0;
    }
}
