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
import lumbermill.http.UnitOfWorkAwareSource;
import lumbermill.http.UnitOfWorkListener;
import lumbermill.internal.MapWrap;
import lumbermill.internal.http.VertxHttpServer;

import java.util.Map;

/**
    Provides an easy to use API for creating HTTP server
     TODO: Consider a way too expose the concept of Routes in the API
 */
public class Http {

    private Http() {}

    public static Http http = new Http();


    private VertxHttpServer httpServer;

    public Server server(Map<String, Object> config) {
        httpServer = new VertxHttpServer<>(MapWrap.of(config));
        return httpServer;
    }


    public interface Server<T extends Event> extends UnitOfWorkAwareSource {

        /**
         * Use tags to decide which type of request to subscribe to instead of using forEach to get all.
         */
        UnitOfWorkAwareSource<T> onTag(String tag, UnitOfWorkListener callback);

        /**
         * Receives all events that are not picked up by onTag() method.
         */
        UnitOfWorkAwareSource<T> on(UnitOfWorkListener callback);

        Server<T> post(Map config);
        Server<T> get(Map config);
    }

}