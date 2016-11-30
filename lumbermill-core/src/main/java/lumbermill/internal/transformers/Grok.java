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
package lumbermill.internal.transformers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import io.thekraken.grok.api.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Extracts fields from logs and adds them to json structure.
 */
public class Grok<E extends Event>  {

    private final Logger LOGGER = LoggerFactory.getLogger(Grok.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Json field to extract value from
     */
    private final String field;

    /**
     * Grok patten to use
     */
    private final String pattern;

    /**
     * Wether we should errorTagName if an parse fails
     */
    private final boolean shouldTag;

    /**
     * Tag to use if parse fails
     */
    private final String errorTagName;

    /**
     * Grok instance
     */
    private final io.thekraken.grok.api.Grok internal;


    /**
     * Use GrokFactory to create
     */
     Grok(io.thekraken.grok.api.Grok internal, String field, String pattern, boolean shouldtag, String errorTagName) {
        this.internal = internal;
        this.field = field;
        this.pattern = pattern;
        this.shouldTag = shouldtag;
        this.errorTagName = errorTagName;
    }

    public Observable<E> parse(E event) {
        return Observable.just(doGrok(event));
    }

    public Observable<List<E>> parse(List<E> events) {
        return Observable.just(events.parallelStream()
                .map(this::doGrok)
                .collect(toList()));
    }


    private E doGrok(E event) {

        if (!(event instanceof JsonEvent)) {
            throw new IllegalStateException("Invalid type for Grok, must be JsonEvent but was " +
                    event.getClass().getSimpleName());
        }
        JsonEvent jsonEvent = (JsonEvent)event;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Grok event {}", event.toString());
        }
        if (!jsonEvent.has(field)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Event did not have requested field: {}, event = {}", field, jsonEvent.toString(false));
            }
            return event;
        }

        String value = jsonEvent.valueAsString(field);
        Match gm = internal.match(value);
        gm.captures();

        if (gm.isNull()) {
            if (shouldTag) {
                jsonEvent.addTag(errorTagName);
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Grok  did not match {}, {}", pattern, value);
            }
            return event;
        }

        try {
            JsonEvent newEvent = new JsonEvent((ObjectNode) objectMapper.readTree(gm.toJson()));
            fixUnsupportedGrokTypes(newEvent);
            jsonEvent.merge(newEvent);
            jsonEvent.removeIfExists(errorTagName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    /**
     * Float is not supported in java-grok, fixing this here.
     */
    private void fixUnsupportedGrokTypes(final JsonEvent event) {
        event.eachField((field1, value) -> {
            if (field1.endsWith(":float")) {
                String[] nameAndType = field1.split(":");
                event.remove(field1);
                event.put(nameAndType[0], Float.parseFloat(value));
            }
        });
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {" +
                "field='" + field + '\'' +
                ", pattern=" + pattern +
                '}';
    }
}
