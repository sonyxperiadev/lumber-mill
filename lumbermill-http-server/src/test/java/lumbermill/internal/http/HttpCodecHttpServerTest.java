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


import lumbermill.http.AbstractHttpHandler;
import okio.ByteString;
import org.junit.Before;
import org.junit.Test;
import lumbermill.api.Codecs;
import lumbermill.Http;
import lumbermill.api.Event;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.await;
import static lumbermill.Core.wrap;

public class HttpCodecHttpServerTest extends AbstractHttpServerTest {

    String contentType = "text/plain";
    String[] postPaths = {"/post"};
    String[] getPaths = {"/get"};

    String postPath = "/post";
    String getPath = "/get";

    Http.Server server;

    @Before
    public void prepare() {
        server =  prepare(TextToJsonHttpHandler.supplier(), postPaths, getPaths);
    }


    @Test
    public void test_post_text_creates_valid_json_for_each_request() throws InterruptedException {
        server.on(observable -> observable.doOnNext(subscriber().action1()));

        post(postPath, "Hello", contentType, 200);

        await().atMost(1, TimeUnit.SECONDS).until(subscriber().onNextInvoked(1));
    }

    @Test
    public void test_post_text_creates_valid_json_for_each_request2() throws InterruptedException {
        server.on(observable -> observable
                .map(Funcs.runtimeEx()).doOnTerminate(subscriber()));

        post(postPath, "Hello", contentType, 500);

        await().atMost(1, TimeUnit.SECONDS).until(subscriber().onCompletedInvoked());
    }

    static class TextToJsonHttpHandler extends AbstractHttpHandler {

        public static Supplier<TextToJsonHttpHandler> supplier() {
            return () -> new TextToJsonHttpHandler();
        }

        @Override
        public Event doParse(HttpPostRequest request) throws HttpCodecException {
            //request.response().write("hello").setStatusCode(200);
            return from(request.message());
        }

        @Override
        public void onNext(Event o) {

        }

        @Override
        public Event from(ByteString b) {
            return Codecs.TEXT_TO_JSON.from(b);
        }

        @Override
        public Event from(byte[] b) {
            return Codecs.TEXT_TO_JSON.from(b);
        }

        @Override
        public Event from(String s) {
            return null;
        }
    }


}
