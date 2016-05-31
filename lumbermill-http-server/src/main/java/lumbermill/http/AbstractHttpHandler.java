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
package lumbermill.http;

import lumbermill.api.Event;


public abstract class AbstractHttpHandler<REQ extends Event, RES extends Event> implements HttpHandler {

    protected HttpPostRequest req;

    @Override
    public final REQ parse(HttpPostRequest req) {
        this.req = req;
        return doParse(req);
    }

    protected abstract REQ doParse(HttpPostRequest req);

    @Override
    public void onCompleted() {
        req.response().setStatusCode(200).end();
    }

    @Override
    public void onError(Throwable e) {
        req.failure();
    }

}
