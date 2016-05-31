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
package lumbermill.internal.http;

import lumbermill.http.HttpHandler;
import lumbermill.http.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.ext.web.RoutingContext;
import rx.Observable;
import rx.Subscriber;
import lumbermill.api.Event;

import java.util.UUID;

public class HttpUnitOfWork<IN extends Event, OUT extends Event> implements UnitOfWork {



    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUnitOfWork.class + "-" + UUID.randomUUID());

    private final Observable<IN> observable;
    private final RoutingContext routingContext;

    HttpUnitOfWork(IN event) {
        routingContext = event.get(PostHandler.EVENT_METADATA_HTTP_ROUTING_CONTEXT);
        observable = Observable.create((Subscriber<? super IN> subscriber) -> {
            try {
                subscriber.onNext(event);
                subscriber.onCompleted();
            } catch (RuntimeException e) {
                subscriber.onError(e);
            }
        });
    }

    @Override
    public Observable<IN> observable() {
        return observable;
    }

    @Override
    public Subscriber<OUT> subscriber() {
        return new Subscriber<OUT>() {
            @Override
            public void onCompleted() {
                LOGGER.debug("On completed invoked");


                LOGGER.trace("Closing request");
                HttpHandler<IN,OUT> httpHandler = routingContext.get("httpHandler");
                if (httpHandler != null) {
                    httpHandler.onCompleted();
                } else {
                    routingContext.response().end();
                }
            }

            @Override
            public void onError(Throwable e) {
                LOGGER.warn("Error received, sending http:500 and closing request", e);
                // TODO: Investigate why does the SubscribingHttpServer test fail if below line is
                // uncommented.
                // routingContext.response().setStatusMessage(e.getMessage());
                HttpHandler<IN,OUT> httpHandler = routingContext.get("httpHandler");
                if (httpHandler != null) {
                    httpHandler.onError(e);
                } else {
                    routingContext.fail(500);
                }
            }

            @Override
            public void onNext(OUT o) {
                HttpHandler<IN,OUT> httpHandler = routingContext.get("httpHandler");
                if (httpHandler != null) {
                    httpHandler.onNext(o);
                }
            }
        };
    }
}