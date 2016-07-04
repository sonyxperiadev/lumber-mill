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
import lumbermill.api.JsonEvent;
import lumbermill.internal.JsonParseException;
import lumbermill.internal.MapWrap;
import org.junit.Test;
import rx.Observable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

// TODO - Move other json related tests here
public class JsonTest {

    static final String DECODED_CLOUDWATCH_LOGS_EVENT_WITH_JSON_MESSAGE = "{\n" +
            "\"id\" : \"32692830297495645171166331613143127184950827531125981184\",\n" +
            "\"timestamp\" : 1465997212490,\n" +
            "\"message\" : " +
            "   \"{\\\"module\\\":\\\"RecordProcessor\\\"," +
            "   \\\"thread\\\":\\\"shardId-000000000003\\\"," +
            "   \\\"message\\\":\\\"Got 12 records (5857 bytes)\\\"," +
            "   \\\"@timestamp\\\":\\\"2016-06-15T13:26:52.485Z\\\"}\",\n" +
            "\"logGroup\" : \"dev-ois-demux-LogGroup-K6USOFCNB7QM\",\n" +
            "\"logStream\" : \"application-ois-demux-latest\",\n" +
            "\"@timestamp\" : \"2016-06-15T13:26:52.49Z\"\n" +
            "} ";

    @Test
    public void test_decode_and_merge_field_from_string_to_json_event() {
        JsonEvent mutatingEvent = Codecs.JSON_OBJECT.from(DECODED_CLOUDWATCH_LOGS_EVENT_WITH_JSON_MESSAGE);
        Core.extractJsonObject("message").call(mutatingEvent);
        assertThat(mutatingEvent.has("thread")).isTrue();
        assertThat(mutatingEvent.has("module")).isTrue();
        assertThat(mutatingEvent.valueAsString("message")).isEqualTo("Got 12 records (5857 bytes)");
        assertThat(mutatingEvent.valueAsString("@timestamp")).isEqualTo("2016-06-15T13:26:52.485Z");
    }

    @Test
    public void test_decode_and_replace_field_from_string_to_json_event() {
        Codecs.JSON_OBJECT.from(DECODED_CLOUDWATCH_LOGS_EVENT_WITH_JSON_MESSAGE)
                .<JsonEvent>toObservable()
                .flatMap (
                        Core.extractJsonObject (
                                MapWrap.of("field","message", "merge", false).toMap())
                )
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("logGroup")).isFalse())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("logGroup")).isFalse())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("logStream")).isFalse())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("thread")).isTrue())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("module")).isTrue())
                .doOnNext (jsonEvent -> assertThat(jsonEvent.valueAsString("@timestamp")).isEqualTo("2016-06-15T13:26:52.485Z"))
                .doOnNext( jsonEvent -> assertThat(jsonEvent.valueAsString("message")).isEqualTo("Got 12 records (5857 bytes)"))
                .subscribe();

    }

    @Test
    public void test_decode_non_json_ignore() {
        Codecs.JSON_OBJECT.from(DECODED_CLOUDWATCH_LOGS_EVENT_WITH_JSON_MESSAGE)
                .<JsonEvent>toObservable()
                .flatMap (
                        Core.extractJsonObject(
                                MapWrap.of("field","logGroup", "merge", false, "ignoreNonJson", true).toMap())
                )
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("logGroup")).isTrue())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("logStream")).isTrue())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("thread")).isFalse())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("module")).isFalse())
                .doOnNext(jsonEvent -> assertThat(jsonEvent.valueAsString("message")).startsWith("{"))
                .doOnNext(jsonEvent -> assertThat(jsonEvent.valueAsString("@timestamp")).isEqualTo("2016-06-15T13:26:52.49Z"))
                .subscribe();
    }

    @Test(expected = JsonParseException.class)
    public void test_decode_non_json_error() {
        JsonEvent mutatingEvent = Codecs.JSON_OBJECT.from(DECODED_CLOUDWATCH_LOGS_EVENT_WITH_JSON_MESSAGE);
        Core.extractJsonObject(
                MapWrap.of("field", "logGroup", "merge", false, "ignoreNonJson", false).toMap()).call(mutatingEvent);
    }


    @Test (expected = JsonParseException.class)
    public void test_parse_failing_json() {
        Observable.just(Codecs.BYTES.from("This is not json"))
                .map(Core.toJsonObject())
                .toBlocking()
                .subscribe();
    }


    @Test
    public void test_parse_failing_json_but_use_text_to_json_if_fails() {
        Codecs.BYTES.from("This is not json")
                .toObservable()
                .flatMap(Core.toJsonObject(MapWrap.of("create_json_on_failure", true).toMap()))
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("message")))
                .doOnNext(jsonEvent -> assertThat(jsonEvent.has("@timestamp")))
                .toBlocking()
                .subscribe();
    }
}
