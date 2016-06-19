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

import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static lumbermill.Core.addField;
import static lumbermill.Core.console;
import static lumbermill.Core.fingerprint;
import static lumbermill.Core.date;
import static lumbermill.Core.grok;
import static lumbermill.Core.ifExists;
import static lumbermill.Core.ifMatch;
import static lumbermill.Core.ifNotExists;
import static lumbermill.Core.ifNotMatch;
import static lumbermill.Core.params;
import static lumbermill.internal.MapWrap.of;
import static org.assertj.core.api.Assertions.assertThat;

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
    public void testIfNotExistsInvokeFlatMap() {
        Observable.just(Codecs.TEXT_TO_JSON.from("Hello there"))
                .flatMap(ifExists("message").flatMap (
                        grok.parse(MapWrap.of("field", "message", "pattern", "%{UUID}").toMap())))
                .doOnNext(console.stdout())
                .subscribe();

//        assertThat(event.hasTag("_grokparsefailure")).isTrue();

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
    public void testIfNotMatchAdd() {
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello there").put("field", "yes");
        ifNotMatch("field", "yes").add(of("found", true).toMap()).call(event);
        assertThat(event.has("found")).isFalse();
        assertThat(event.has("found")).isFalse();

        ifNotMatch("field", "no").add(of("foundagain", true).toMap()).call(event);
        assertThat(event.has("foundagain")).isTrue();
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


    @Test
    public void test_fingerprint_with_contents() {
        Event event  = Codecs.BYTES.from("Hello");
        Event event2 = Codecs.BYTES.from("Hello")
                /* Metadata not part of contents*/.put("data", "data");

        assertThat(
                fingerprint.md5().call(event).valueAsString("fingerprint"))
                .isEqualTo(
                        fingerprint.md5().call(event2).valueAsString("fingerprint"));

        // Lets verify that they do not match if they differ :-)
        Event jsonEvent3 = Codecs.BYTES.from("HelloWorld");

        assertThat (
                fingerprint.md5().call(event).valueAsString("fingerprint"))
                .isNotEqualTo(
                        fingerprint.md5().call(jsonEvent3).valueAsString("fingerprint"));
    }

    @Test
    public void test_fingerprint_with_fields_match() {
        JsonEvent jsonEvent  = Codecs.TEXT_TO_JSON.from("Hello");
        JsonEvent jsonEvent2 = Codecs.TEXT_TO_JSON.from("Hello").put("data", "data");

        // Using only message
        Func1<JsonEvent, JsonEvent> checksumFunction = fingerprint.md5("{message}");

        assertThat(
                checksumFunction.call(jsonEvent).valueAsString("fingerprint"))
                    .isEqualTo(
                            checksumFunction.call(jsonEvent2).valueAsString("fingerprint"));

        // Sanity, Make sure it is a correct hash
        assertThat(checksumFunction.call(jsonEvent).valueAsString("fingerprint"))
                .isEqualTo("8b1a9953c4611296a827abf8c47804d7");

        // Sanity, should be stored as metadata, make sure it is not there on raw copy
        JsonEvent rawCopy = Codecs.JSON_OBJECT.from(jsonEvent.raw());
        assertThat(rawCopy.has("fingerprint")).isFalse();
    }

    @Test
    public void test_checksum_does_not_match_when_field_is_missing_in_one() {
        JsonEvent jsonEvent  = Codecs.TEXT_TO_JSON.from("Hello");
        JsonEvent jsonEvent2 = Codecs.TEXT_TO_JSON.from("Hello").put("data", "data");

        // Data only occurs in second
        Func1<JsonEvent, JsonEvent> checksumFunction = fingerprint.md5("{message}{data}");

        assertThat(
                checksumFunction.call(jsonEvent).valueAsString("fingerprint"))
                .isNotEqualTo(
                        checksumFunction.call(jsonEvent2).valueAsString("fingerprint"));
    }

    @Test
    public void test_checksum_should_not_match() {
        JsonEvent jsonEvent  = Codecs.TEXT_TO_JSON.from("Hello").put("data", "world");
        JsonEvent jsonEvent2 = Codecs.TEXT_TO_JSON.from("Hello").put("data", "World");

        Func1<JsonEvent, JsonEvent> checksumFunction = fingerprint.md5("{message}|{data}");

        assertThat(
                checksumFunction.call(jsonEvent).valueAsString("fingerprint"))
                .isNotEqualTo(
                        checksumFunction.call(jsonEvent2).valueAsString("fingerprint"));
    }
}
