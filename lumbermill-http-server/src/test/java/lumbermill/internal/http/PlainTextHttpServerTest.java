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

import lumbermill.Http;
import lumbermill.api.Event;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;


public class PlainTextHttpServerTest<T extends Event> extends AbstractHttpServerTest {

    public static final String TAG = "thetag";
    String contentType = "text/plain";
    String path = "/post";

    Http.Server<T> server = null;

    @Before
    public void prepare() {
        server = withTags(TAG).prepare(path);
    }


    @Test
    public void test_tag_is_correct_with_each() {

        server.on(observable -> observable
                .filter(t -> t.hasTag(TAG))
                .doOnNext(subscriber().action1()));

        post(path, "Hello", contentType, 200);

        await().atMost(1, TimeUnit.SECONDS).until(subscriber().onNextInvoked(1));
        assertThat(subscriber.lastEvent().hasTag(TAG)).isTrue();
    }

}
