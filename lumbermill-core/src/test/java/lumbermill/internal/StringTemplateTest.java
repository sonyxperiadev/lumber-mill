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
package lumbermill.internal;


import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.collect.ImmutableMap;
import lumbermill.Core;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class StringTemplateTest {

    @Before
    public void prepare() {
        System.setProperty("field", "value");
    }

    @After
    public void after() {
        System.setProperty("field","");
    }

    @Test
    public void testExtractSingleValue() {
        StringTemplate t = StringTemplate.compile("{field}");
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello").put("field", "value");
        Optional<String> formattedString = t.format(event);
        assertThat(formattedString.isPresent()).isTrue();
        assertThat(formattedString.get()).isEqualTo("value");
    }


    @Test
    public void testExtractNestedSingleValue() {
        StringTemplate t = StringTemplate.compile("{/key/yes}");
        JsonEvent event = Codecs.JSON_OBJECT.from("{\"key\":{\"yes\":true}}");
        Optional<String> formattedString = t.format(event);
        assertThat(formattedString.isPresent()).isTrue();
        assertThat(formattedString.get()).isEqualTo("true");
    }

    @Test
    public void testExtractNestedDefaultValue() {
        StringTemplate t = StringTemplate.compile("{/key/yes || false}");
        JsonEvent event = Codecs.JSON_OBJECT.from("{\"key\":{\"yes\":true}}");
        Optional<String> formattedString = t.format(event);
        assertThat(formattedString.isPresent()).isTrue();
        assertThat(formattedString.get()).isEqualTo("true");
    }

    @Test
    public void testExtractNestedDefaultValueNotExist() {
        StringTemplate t = StringTemplate.compile("{/key/yes || false}");
        JsonEvent event = Codecs.JSON_OBJECT.from("{\"key\":{\"no\":true}}");
        Optional<String> formattedString = t.format(event);
        assertThat(formattedString.isPresent()).isTrue();
        assertThat(formattedString.get()).isEqualTo("false");
    }


    @Test
    public void testExtractSentence() {
        try {

            StringTemplate t = StringTemplate.compile("Hej hopp {field}");
            Event event = Codecs.BYTES.from("Hello").put("field", "value");
            Optional<String> formattedString = t.format(event);
            assertThat(formattedString.isPresent()).isTrue();
            assertThat(formattedString.get()).isEqualTo("Hej hopp value");

            ObjectNode objectNode = new ObjectNode(JsonNodeFactory.instance);
            JsonEvent jsonEvent = new JsonEvent(objectNode);
            jsonEvent.put("id", "This is a test ID");
            jsonEvent.put("version", "This is a test version");

            StringTemplate key = StringTemplate.compile("{id}/{version}");
            assertThat(key.format(jsonEvent).get()).isEqualTo("This is a test ID/This is a test version");

        }
        catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testExtractSystemProperty() {

        Codecs.TEXT_TO_JSON.from("hello world")
                .<JsonEvent>toObservable()
                .flatMap(Core.addField("field", "Hej hopp {field}"))
                .doOnNext( o -> assertThat(o.valueAsString("field")).isEqualTo("Hej hopp value"))
                .subscribe();
    }

    @Test
    public void testExtractSystemPropertyOnly() {

        StringTemplate template = StringTemplate.compile("{field}");
        assertThat(template.format().get()).isEqualTo("value");
    }

    @Test
    public void testExtractSystemPropertyWithDefaultButValueFound() {

        StringTemplate template = StringTemplate.compile("{field||monkey}");
        assertThat(template.format().get()).isEqualTo("value");

    }

    @Test
    public void testExtractSystemPropertyWithDefaultValue() {
        StringTemplate template = StringTemplate.compile("{field2||monkey}");
        assertThat(template.format().get()).isEqualTo("monkey");

        template = StringTemplate.compile("That is a {field2||monkey}");
        assertThat(template.format().get()).isEqualTo("That is a monkey");

        template = StringTemplate.compile("That is a {field2 || monkey}");
        assertThat(template.format().get()).isEqualTo("That is a monkey");
    }

    @Test
    public void testExtractSystemPropertyWithDefaultValueIsEmptyString() {
        StringTemplate template = StringTemplate.compile("{field2|| }");
        assertThat(template.format().get()).isEqualTo("");

        template = StringTemplate.compile("This '{field2|| }' should be empty");
        assertThat(template.format().get()).isEqualTo("This '' should be empty");
    }

    /**
     * This test displays that when using conditional field it will be evaluated when setting
     * up the pipeline and not for each event.
     */
    @Test
    public void testTemplateWithEventWorksAsExpected() {

        Codecs.TEXT_TO_JSON.from("data")
                .<JsonEvent>toObservable()
                .flatMap (Core.rename(ImmutableMap.of("from", "message", "to", "{field || monkey}")))
                .doOnNext(ev -> assertThat(ev.valueAsString("value")).isEqualTo("data"))
                .subscribe();

        // Event though we supply field2:banana, this is not known when the rename function is created
        // so the default 'monkey' is used.
        Codecs.TEXT_TO_JSON.from("data").put("field2", "banana")
                .<JsonEvent>toObservable()
                .flatMap (Core.rename(ImmutableMap.of("from", "message", "to", "{field2 || monkey}")))
                .doOnNext(ev -> assertThat(ev.valueAsString("monkey")).isEqualTo("data"))
                .subscribe();

    }


}
