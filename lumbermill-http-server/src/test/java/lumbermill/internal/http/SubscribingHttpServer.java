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

import lumbermill.Core;
import lumbermill.api.Event;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import lumbermill.Http;
import rx.Observable;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static lumbermill.internal.http.Funcs.runtimeEx;

@Ignore("Does not work on Travis for some reason")
public class SubscribingHttpServer extends AbstractHttpServerTest {

    public static final String TAG = "tag";
    String contentType = "text/plain";
    String path = "/post";

    Http.Server server = null;

    @Before
    public void prepare() {
        server = withTags(TAG).prepare(path);
    }

    @Test
    public void test_on_tag_with_subscriber_closes_properly() throws InterruptedException {
        server.onTag(TAG, observable -> observable);
        post(path, "{\"Hello\":true}", contentType, 200);
    }

    @Test
    public void test_foreach_with_subscriber_closes_properly() throws InterruptedException {
        server.on(observable -> observable);
        post(path, "{\"Hello\":true}", contentType, 200);
    }

    @Test
    public void test_exception_during_pipeline_returns_500_and_error_to_subscriber() throws InterruptedException {
        server.on(observable -> {

                    observable = observable
                        .map(Core.wrap(runtimeEx()));
                    observable.subscribe(subscriber());
            return observable; }

        );

        post(path, "Hello", contentType, 500);

        await().atMost(1, TimeUnit.SECONDS).until(subscriber.onErrorInvoked());
    }

}
