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

import org.junit.Test;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class StringTemplateTest {

    @Test
    public void testExtractSingleValue() {
        StringTemplate t = StringTemplate.compile("{field}");
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello").put("field", "value");
        Optional<String> formattedString = t.format(event);
        assertThat(formattedString.isPresent()).isTrue();
        assertThat(formattedString.get()).isEqualTo("value");
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


}
