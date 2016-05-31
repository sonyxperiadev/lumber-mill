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

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lumbermill.api.Codec;
import lumbermill.api.Event;
import okio.ByteString;


public interface HttpHandler<REQ extends Event, RES extends Event> extends Codec {

    REQ parse(HttpPostRequest request) throws HttpCodecException;

    void onCompleted();
    void onError(Throwable e);
    void onNext(RES o);

    class HttpCodecException extends RuntimeException {

        public final int status;
        public final String reason;

        public HttpCodecException(int status, String reason) {
            super(reason);
            this.status = status;
            this.reason = reason;
        }
    }

    interface HttpPostRequest {

        HttpServerResponse response();
        HttpServerRequest request();
        ByteString message();

        void failure();
    }
}
