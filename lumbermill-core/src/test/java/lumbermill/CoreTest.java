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

import org.junit.Test;
import lumbermill.api.Codecs;
import lumbermill.api.JsonEvent;
import rx.Observable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static lumbermill.internal.MapWrap.of;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static lumbermill.Core.addField;
import static lumbermill.Core.date;
import static lumbermill.Core.ifExists;
import static lumbermill.Core.ifMatch;
import static lumbermill.Core.ifNotExists;
import static lumbermill.Core.params;

public class CoreTest {

    @Test
    public void testDate() {
        JsonEvent event = Codecs.JSON_OBJECT.from("{\"time\":\"2016-01-01T22\"}");
        JsonEvent eventWithRenamedTime = date("time", "yyyy-MM-dd'T'HH").call(event);
        assertThat(eventWithRenamedTime.valueAsString("@timestamp")).isEqualTo("2016-01-01T22:00:00Z");
    }

    @Test
    public void testIfExistsAdd() {
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello there");
        ifExists("message").add(of("found", true).toMap()).call(event);
        assertThat(event.has("found")).isTrue();
        assertThat(event.asBoolean("found")).isTrue();

        ifExists("nonexisting").add(of("foundagain", true).toMap()).call(event);
        assertThat(event.has("foundagain")).isFalse();
    }

    @Test
    public void testIfExistsInvoke() {
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello there");
        ifExists("message").map(addField("found", true)).call(event);
        assertThat(event.has("found")).isTrue();
        assertThat(event.asBoolean("found")).isTrue();

        ifExists("nonexisting").map(addField("foundagain", true)).call(event);
        assertThat(event.has("foundagain")).isFalse();
    }

    @Test
    public void testIfNotExistsInvoke() {
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello there");
        ifNotExists("message").map(addField("found", true)).call(event);
        assertThat(event.has("found")).isFalse();

        ifNotExists("nonexisting").map(addField("foundagain", true)).call(event);
        assertThat(event.has("foundagain")).isTrue();
    }

    @Test
    public void testIfMatchAdd() {
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello there").put("field", "yes");
        ifMatch("field", "yes").add(of("found", true).toMap()).call(event);
        assertThat(event.has("found")).isTrue();
        assertThat(event.asBoolean("found")).isTrue();

        ifMatch("field", "no").add(of("foundagain", true).toMap()).call(event);
        assertThat(event.has("foundagain")).isFalse();
    }

    @Test
    public void testParamExtraction() {

        JsonEvent event = Codecs.TEXT_TO_JSON.from("https://platform.lifelog.sonymobile.com:443/oauth/2/refresh_token?param=1&param2=hej");
        params(of("field","message", "names", asList("param")).toMap()).call(event);
        assertThat(event.valueAsString("param")).isEqualTo("1");
        assertThat(event.has("param2")).isFalse();

        event = Codecs.TEXT_TO_JSON.from("param1&param2=hej");
        params(of("field", "message", "names", asList("param2")).toMap()).call(event);
        assertThat(event.valueAsString("param2")).isEqualTo("hej");
        assertThat(event.has("param")).isFalse();

        // Just make sure it does not crash
        event = Codecs.TEXT_TO_JSON.from("https://platform.lifelog.sonymobile.com:443/oauth/2/refresh_token?asdfasdf?=asdfasdf&adasdf");
        params(of("field", "message", "names", asList("param")).toMap()).call(event);

        event = Codecs.TEXT_TO_JSON.from("https://platform.lifelog.sonymobile.com:443/oauth/2/refresh_token?param=1&param2=hej");
        params(of("field", "message", "names", new ArrayList<>()).toMap()).call(event);
        assertThat(event.valueAsString("param")).isEqualTo("1");
        assertThat(event.valueAsString("param2")).isEqualTo("hej");


    }
}
