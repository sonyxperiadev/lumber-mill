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
import org.junit.Test;
import rx.Subscriber;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static lumbermill.Core.*;
import static lumbermill.internal.MapWrap.of;
import static org.assertj.core.api.Assertions.assertThat;

public class CoreTest {

    @Test
    public void testDate() {
        Codecs.JSON_OBJECT.from("{\"time\":\"2016-01-01T22\"}")
                .<JsonEvent>toObservable()
                .flatMap (
                        date("time", "yyyy-MM-dd'T'HH"))
                .doOnNext(jsonEvent ->
                        assertThat(jsonEvent.valueAsString("@timestamp")).isEqualTo("2016-01-01T22:00:00Z"))
                .subscribe();
    }


    @Test
    public void testConditionalSanityFromJava() {

        Codecs.TEXT_TO_JSON.from("Hello there")
                .<JsonEvent>toObservable()
                .flatMap (
                        computeIfExists("message", addField("found", true))
                )
                .doOnNext(jsonEvent -> assertThat(jsonEvent.asBoolean("found")).isTrue())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("found")).isTrue())
                .flatMap (
                        computeIfExists("nonexisting", addField("foundagain", true))
                )
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("foundagain")).isFalse())
                .subscribe();
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

        event.toObservable().flatMap (fingerprint.md5()).subscribe();

        event2.toObservable().flatMap (fingerprint.md5()).subscribe();


        assertThat(event.valueAsString("fingerprint"))
                    .isEqualTo(
                        event2.valueAsString("fingerprint"));


        // Lets verify that they do not match if they differ :-)
        Event jsonEvent3 = Codecs.BYTES.from("HelloWorld");
        jsonEvent3.toObservable().flatMap( fingerprint.md5()).subscribe();

        assertThat (event.valueAsString("fingerprint"))
                .isNotEqualTo(
                        jsonEvent3.valueAsString("fingerprint"));
    }

    @Test
    public void test_fingerprint_with_fields_match() {
        JsonEvent jsonEvent  = Codecs.TEXT_TO_JSON.from("Hello");
        JsonEvent jsonEvent2 = Codecs.TEXT_TO_JSON.from("Hello").put("data", "data");

        jsonEvent.toObservable().flatMap (fingerprint.md5("{message}")).subscribe();

        jsonEvent2.toObservable().flatMap (fingerprint.md5("{message}")).subscribe();

        assertThat(jsonEvent.valueAsString("fingerprint"))
                .isEqualTo(
                        jsonEvent2.valueAsString("fingerprint"));

        // Sanity, Make sure it is a correct hash
        assertThat(jsonEvent.valueAsString("fingerprint"))
                .isEqualTo("8b1a9953c4611296a827abf8c47804d7");

        // Sanity, should be stored as metadata, make sure it is not there on raw copy
        JsonEvent rawCopy = Codecs.JSON_OBJECT.from(jsonEvent.raw());
        assertThat(rawCopy.has("fingerprint")).isFalse();
    }

    @Test
    public void test_checksum_exception_is_thrown_when_field_is_missing_in_one() {

        AtomicReference<Throwable> expected = new AtomicReference<>();

        rx.Observable.<Event>merge(
                Codecs.TEXT_TO_JSON.from("Hello").toObservable(),
                Codecs.TEXT_TO_JSON.from("Hello").put("data", "data").toObservable())
                .flatMap (fingerprint.md5("{message}{data}"))
                .buffer(2)
                .doOnNext(events -> assertThat(events.get(0).valueAsString("fingerprint")).
                        isNotEqualTo(events.get(1).valueAsString("fingerprint")))
                .subscribe(new Subscriber<List<Event>>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        expected.set(e);
                    }

                    @Override
                    public void onNext(List<Event> events) {

                    }
                });
        assertThat(expected.get()).isNotNull();
        assertThat(expected.get().getClass()).isEqualTo(IllegalStateException.class);
    }

    @Test
    public void test_checksum_should_not_match() {

        rx.Observable.<Event>merge(
                Codecs.TEXT_TO_JSON.from("Hello").put("data", "world").toObservable(),
                Codecs.TEXT_TO_JSON.from("Hello").put("data", "World").toObservable())

                .flatMap (fingerprint.md5("{message}{data}"))
                .buffer(2)
                .doOnNext(events -> assertThat(events.get(0).valueAsString("fingerprint")).
                        isNotEqualTo(events.get(1).valueAsString("fingerprint")))
                .subscribe();

    }
}
