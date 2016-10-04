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
package lumbermill;

import lumbermill.api.Event;
import rx.Subscriber;
import rx.functions.Action;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AssertableSubscriber<T extends Event> extends Subscriber<T> implements Action, Action0 {

    public AtomicBoolean completed = new AtomicBoolean(false);
    public AtomicBoolean error = new AtomicBoolean(false);
    public AtomicInteger onNextCounter = new AtomicInteger(0);
    public AtomicReference<T> lastEvent = new AtomicReference<>();

    @Override
    public void onCompleted() {
        System.out.println("OK!");
        completed.set(true);
    }

    @Override
    public void onError(Throwable e) {
        error.set(true);
    }

    @Override
    public void onNext(T o) {
        System.out.println("IIIIEEE");
        onNextCounter.incrementAndGet();
        lastEvent.set(o);
    }

    public Callable<Boolean> onCompletedInvoked() {
        return () -> completed.get();
    }

    public Callable<Boolean> onNextInvoked(int times) {
        return () -> onNextCounter.get() == times;
    }

    public Callable<Boolean> onErrorInvoked() {
        return () -> error.get() == true;
    }

    public  <E extends Event> E lastEventAs(Class<E> c) {
        return (E)lastEvent.get();
    }

    public  T lastEvent() {
        return lastEvent.get();
    }

    @Override
    public void call() {
        onCompleted();
    }


    public Action1<T> action1() {
        return t -> onNext(t);
    }
}
