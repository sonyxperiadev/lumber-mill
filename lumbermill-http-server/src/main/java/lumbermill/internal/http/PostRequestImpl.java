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

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import lumbermill.http.HttpHandler;
import okio.ByteString;

public class PostRequestImpl implements HttpHandler.HttpPostRequest {

    private final RoutingContext routingContext;

    public PostRequestImpl(RoutingContext context) {
        this.routingContext = context;
        this.routingContext.response().setChunked(true);
    }

    @Override
    public HttpServerResponse response() {
        return routingContext.response();
    }

    @Override
    public HttpServerRequest request() {
        return routingContext.request();
    }

    @Override
    public ByteString message() {
        return ByteString.of(routingContext.getBody().getBytes());
    }

    @Override
    public void failure() {
        this.routingContext.fail(500);
    }
}