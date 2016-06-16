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

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lumbermill.Http;
import lumbermill.api.Codec;
import lumbermill.api.Event;
import lumbermill.internal.MapWrap;
import lumbermill.api.MetaDataEvent;
import lumbermill.http.UnitOfWork;
import lumbermill.http.UnitOfWorkListener;
import lumbermill.internal.http.PostHandler.OnPostCreatedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.stream.Collectors.toList;


/**
 * HttpServer used to receive events over http.
 */
public class VertxHttpServer<T extends Event> extends AbstractVerticle implements Http.Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(VertxHttpServer.class);

    private static final int DEFAULT_PORT = 5678;
    private static final String ON_NO_TAG = "_totag";
    private final int port;
    private final BlockingQueue<Event> queue;
    private final Router router;
    private final Vertx vertx;
    private final HttpServer httpServer;
    private final Map<String, UnitOfWorkListener> observableListenersByTag;

    private boolean setupCompleted;

    public VertxHttpServer(MapWrap config) {
        port = config.get("port", DEFAULT_PORT);
        queue = new LinkedBlockingQueue<>();
        observableListenersByTag = new HashMap<>();

        // Setup vertx
        vertx = Vertx.vertx();
        vertx.deployVerticle(this);
        if (config.exists("keyStorePath") ) {
            httpServer = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
                    .setKeyStoreOptions(
                            new JksOptions().
                                    setPath(config.asString("keyStorePath")).
                                    setPassword(config.asString("keyStorePassword"))
                    ));
        } else {
            httpServer = vertx.createHttpServer();
        }

        router = Router.router(vertx);
    }

    public VertxHttpServer(int port) {
        this(MapWrap.of("port", port));
    }

    @Override
    public Http.Server onTag(String tag, UnitOfWorkListener callback) {
        this.observableListenersByTag.put(tag, callback);
        // Fixme - Ugly...
        if (!setupCompleted) {
            finalizeHttpServer();
        }
        return this;
    }

    @Override
    public Http.Server<T> on(UnitOfWorkListener unitOfWorkListener) {
        this.observableListenersByTag.put(ON_NO_TAG, unitOfWorkListener);
        if (!setupCompleted) {
            finalizeHttpServer();
        }
        return this;
    }

    @Override
    public Http.Server post(Map config) {
        return setupRoute(MapWrap.of(config).put("method", "POST"));
    }

    @Override
    public Http.Server get(Map config) {
        return setupRoute(MapWrap.of(config).put("method", "GET"));
    }

    public void shutdown() {
        httpServer.close();
        vertx.close();
    }

    private Http.Server setupRoute(MapWrap config) {
        String path   = config.asString("path");
        String method = config.asString("method");
        Optional<Codec<T>> codec = config.getIfExists("codec");
        Optional<List<String>> tags = config.getIfExists("tags");

        LOGGER.debug("Setting up route, path: {}, method: {}", path, method);

        if (method.equalsIgnoreCase("POST")) {
            router.route().handler(BodyHandler.create());
            router.post(path).handler(new PostHandler(codec, new PostCreatedCallback(tags)));
        } else if (method.equalsIgnoreCase("GET")) {
            router.get().path(path).handler(new GetHandler());
        }

        return this;
    }

    /**
     * Called after all routes are setup
     */
    private void finalizeHttpServer() {
        if (setupCompleted) {
            throw new IllegalStateException("You can only invoke stream() or forEach() " +
                    "once and either of them, not both");
        }
        // Default route
        router.route().handler(ctx -> ctx.response().setStatusCode(404).end());
        httpServer.requestHandler(router::accept).listen(port);
        setupCompleted = true;
    }

    /**
     * Handles the Event that was parsed in the POST.
     * Depending onTag stream() or each() this will perform the correct action onTag the event
     */
    private class PostCreatedCallback implements OnPostCreatedCallback {

        private final Optional<List<String>> tags;

        public PostCreatedCallback(Optional<List<String>> tags) {
            this.tags = tags;
        }

        @Override
        public void onCreated(Event event) {
            if (tags.isPresent()) {
                ((MetaDataEvent)event).addTags(tags.get());
            }

            List<UnitOfWork<Event>> unitOfWorkListeners =
                     observableListenersByTag.keySet()
                    .stream()
                    .filter(tag -> event.hasTag(tag) || observableListenersByTag.containsKey(ON_NO_TAG))
                    .map(tag -> observableListenersByTag.get(tag))
                    .map(listener -> prepare(event,listener))
                    .collect(toList());

            if (unitOfWorkListeners.isEmpty()) {
                throw new IllegalStateException("No UnitOfWorkListener matches the specified " +
                        "event. Check your tags if using server.onTag('tag'..), or add a " +
                        "fallback with onTag(..)");
            }
        }

        UnitOfWork<Event> prepare(Event event, UnitOfWorkListener listener) {
            HttpUnitOfWork<Event, Event> uow = new HttpUnitOfWork<>(event);
            listener.apply(uow.observable()).subscribe(uow.subscriber());
            return uow;
        }

    }
}