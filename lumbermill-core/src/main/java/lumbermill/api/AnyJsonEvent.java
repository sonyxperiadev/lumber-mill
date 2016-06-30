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
package lumbermill.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okio.ByteString;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

/**
 * Any Json means that it holds either a Json Object or a Json array.
 * Use AnyJsonEvent.each() method to get each event if array and the single node
 * if single json object
 */
public class AnyJsonEvent extends MetaDataEvent {

    private final JsonNode objectNodeOrArrayNode;

    public AnyJsonEvent(JsonNode objectNodeOrArrayNode) {
        this.objectNodeOrArrayNode = objectNodeOrArrayNode;
    }

    public static AnyJsonEvent fromJsonEvents(List<JsonEvent> events) {
        return new AnyJsonEvent(Codecs.objectMapper.createArrayNode()
                .addAll(events.stream()
                .map(jsonEvent -> jsonEvent.jsonNode)
                .collect(toList())));
    }

    public Observable<JsonEvent> each() {
        List<JsonEvent> list = new ArrayList<>();
        if (objectNodeOrArrayNode instanceof ObjectNode) {
            return Observable.from(asList(new JsonEvent((ObjectNode) objectNodeOrArrayNode)));
        }

        objectNodeOrArrayNode.forEach(node -> list.add(new JsonEvent((ObjectNode) node)));
        return Observable.from(list);
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean pretty)
    {
        try {
            return pretty ? JsonEvent.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectNodeOrArrayNode)
                    : JsonEvent.objectMapper.writeValueAsString(objectNodeOrArrayNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteString raw() {
        return ByteString.encodeUtf8(toString());
    }
}
