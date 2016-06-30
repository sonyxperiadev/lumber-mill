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

import lumbermill.api.Codec;
import lumbermill.api.Event;

import java.util.function.Supplier;


/**
 * Abstract class writing a custom http handler. Use this if you really need to customize
 * they way you parse or if you need custom response codes for success or errors.
 *
 * Subclasses MUST also implement Supplier interface, get() is invoked for each method and MUST return
 * a new instance of the handler for each invocation.
 *
 * @param <REQ> Event type to parse into from request
 * @param <RES> Type to expect in onNext() (response) once the processing is completed
 */
public abstract class AbstractHttpHandler<REQ extends Event, RES> implements Supplier<HttpHandler<REQ, RES>>, HttpHandler<REQ, RES> {

    /**
     * Target codec to use
     */
    protected final Codec<REQ> codec;

    /**
     * The http request associated with this handler
     */
    protected HttpPostRequest req;

    protected AbstractHttpHandler(Codec<REQ> codec) {
        this.codec = codec;
    }

    /**
     * This method is final, override doParse to perform custom parsing
     * @param req - Is the current http request
     * @return - An Event
     */
    @Override
    public final REQ parse(HttpPostRequest req) throws HttpCodecException {
        this.req = req;
        return doParse(req);
    }

    /**
     * Override to perform custom parsing and/or validation, null is discouraged to return.
     * @param req - Http request
     * @return - An Event
     */
    protected REQ doParse(HttpPostRequest req) throws HttpCodecException {
        return codec.from(req.message());
    }

    /**
     * By default sends 200 OK once the request is finished, override to customize
     */
    @Override
    public void onCompleted() {
        req.response().setStatusCode(200).end();
    }

    /**
     * By default sends 500 Internal Server Error once, override to customize
     */
    @Override
    public void onError(Throwable e) {
        req.failure();
    }

    /**
     * Does nothing, override to customize
     * @param next - could be an event or buffered events (List)
     */
    @Override
    public void onNext(RES next) {

    }

}
