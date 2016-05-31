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

import com.fasterxml.jackson.core.JsonParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import lumbermill.api.Codecs;
import lumbermill.api.Codec;
import lumbermill.api.Event;
import lumbermill.http.HttpHandler;

public class PostHandler<IN extends Event, OUT extends Event> implements Handler<RoutingContext> {

    public interface OnPostCreatedCallback<T extends Event> {
        void onCreated(T event);
    }

    public static final String EVENT_METADATA_HTTP_ROUTING_CONTEXT = "http.context";

    private static final Logger LOGGER = LoggerFactory.getLogger(PostHandler.class);
    private static final Map<String, Codec> codecs = new HashMap<String, Codec>() {
        {
            put("application/json", Codecs.JSON_ANY);
            put("text/plain", Codecs.TEXT_TO_JSON);
            put("default", Codecs.TEXT_TO_JSON);
        }
    };

    private final Optional<Supplier<Codec<IN>>> codec;
    private final OnPostCreatedCallback callback;

    public PostHandler(Optional<Supplier<Codec<IN>>> codec, OnPostCreatedCallback callback) {
        this.codec = codec;
        this.callback = callback;

    }

    public void handle(RoutingContext context) {
        try {
            String contentType = contentType(context.request());
            Codec<IN> codec = codecFor(contentType);

            LOGGER.trace("Using Content-Type {} and path {}", contentType, context.request().path());

            IN event = parseRequest(context, codec);
            if (event == null) {
                LOGGER.debug("Codec/Handler failed to return an Event from parse, Handler should have closed resources.");
            } else {
                callback.onCreated(event);
            }
        } catch (HttpHandler.HttpCodecException e) {
            LOGGER.warn("Codec failed to handle exception with reason {}", e.reason, e);
            context.fail(e.status);
        } catch (Exception e) {
            if (isUndefinedClientError(e)) {
                LOGGER.warn("Undefined client error, status:400", e);
                context.fail(400);
            } else {
                LOGGER.error("Unknown error, status:500", e);
                context.fail(500);
            }
        }

    }

    private boolean isUndefinedClientError(Exception e) {
        return e instanceof JsonParseException
                || e.getCause() instanceof JsonParseException
                || e instanceof IllegalArgumentException;
    }

    private IN parseRequest(RoutingContext context, Codec<IN> codec) {
        Buffer buffer = context.getBody();

        IN e;
        if (codec instanceof HttpHandler) {
            PostRequestImpl postRequest = new PostRequestImpl(context);
            context.put("postRequest", postRequest);

            e = ((HttpHandler<IN, OUT>) codec).parse(postRequest);

            context.put("httpHandler", codec);
        } else {
            e = codec.from(buffer.getBytes());
        }
        if (e != null) {
            extractRouteParamsAsMetadata(context, e);
            e.put(EVENT_METADATA_HTTP_ROUTING_CONTEXT, context);
        }
        return e;
    }

    private void extractRouteParamsAsMetadata(RoutingContext context, IN e) {
        List<Entry<String, String>> entries = context.request().params().entries();
        entries.forEach(entry -> e.put(entry.getKey(), entry.getValue()));
    }

    private String contentType(HttpServerRequest request) {
        String contentType = request.getHeader("Content-Type");
        if (contentType == null) {
            LOGGER.trace("No Content-Type sent, using text/plain");
            contentType = "text/plain";
        }
        return contentType;
    }

    private Codec<IN> codecFor(String contentTypeHeader) {
        Codec<IN> codec;
        if (this.codec.isPresent()) {
            // Create new Codec
            codec = this.codec.get().get();
        } else {
            codec = codecs.containsKey(contentTypeHeader) ?
                    codecs.get(contentTypeHeader) :
                    codecs.get("default");
        }
        return codec;
    }
}