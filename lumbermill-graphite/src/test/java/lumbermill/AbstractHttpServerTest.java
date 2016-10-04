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


import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import lumbermill.Http;
import lumbermill.api.Codec;
import lumbermill.api.Event;
import lumbermill.internal.MapWrap;
import lumbermill.internal.http.VertxHttpServer;
import org.awaitility.Awaitility;
import org.junit.After;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.util.Arrays.asList;

public class AbstractHttpServerTest<T extends Event>{

    Vertx v = Vertx.vertx();
    HttpClient client = v.createHttpClient();

    VertxHttpServer server;

    AssertableSubscriber<T> subscriber;
    private List<String> tags = new ArrayList<>();


    public <T extends AbstractHttpServerTest> T withTags(String...tags) {
        this.tags = asList(tags);
        return (T) this;
    }

    public Http.Server prepare(String... paths) {

        if (server != null) {
            throw new RuntimeException("Can only create a single server");
        }
        subscriber = new AssertableSubscriber();
        server = new VertxHttpServer(MapWrap.of("port", 9876));

        for (String path : paths) {
            server.post(new HashMap<>(MapWrap.of("path", path,"tags", tags).toMap()));
        }
        return server;
    }

    public Http.Server prepare(Supplier<Codec> codec, String[] postPaths, String[] getPaths) {

        if (server != null) {
            throw new RuntimeException("Can only create a single server");
        }
        subscriber = new AssertableSubscriber();
        server = new VertxHttpServer(MapWrap.of("port", 9876));

        for (String path : postPaths) {
            server.post(new HashMap<>(
                    MapWrap.of("path", path, "handler", codec).toMap()));
        }

        for (String path : getPaths) {
            server.get(new HashMap<>(
                    MapWrap.of("path", path, "handler", codec).toMap()));
        }

        return server;
    }

    @After
    public void shutdown() {
        server.shutdown();
        server = null;
        tags = new ArrayList<>();
    }

    protected AssertableSubscriber<T> subscriber() {
        return subscriber;
    }

    protected void post(String path, String contents, String contentType, int expectedStatusCode){

        AtomicInteger statusCode = new AtomicInteger(-1);

        client.post(9876, "localhost", path, event -> {
            System.out.println(event);
            statusCode.set(event.statusCode());
        })
                .setChunked(true)
                .putHeader("Content-Type", contentType)
                .write(contents).end();

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            System.out.println(statusCode.get());return statusCode.get() == expectedStatusCode;});
    }

    protected void get(String path, String contents, String contentType, int expectedStatusCode) {
        AtomicInteger statusCode = new AtomicInteger(-1);
        client.get(9876, "localhost", path, event -> statusCode.set(event.statusCode()))
                .setChunked(true)
                .putHeader("Content-Type", contentType)
                .write(contents).end();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> statusCode.get() == expectedStatusCode);
    }


}
