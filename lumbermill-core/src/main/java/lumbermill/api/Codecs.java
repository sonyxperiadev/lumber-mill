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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lumbermill.api.BytesEvent;
import lumbermill.api.Codec;

import lumbermill.api.Event;
import lumbermill.api.AnyJsonEvent;
import lumbermill.api.JsonEvent;
import okio.ByteString;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

import static java.time.ZonedDateTime.now;

/**
 * Core codecs
 */
public class Codecs {

    public  static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reads any text into json field 'message' and also adds '@timestamp' to now().
     * This is similar to logstash 'raw' codec.
     */
    public static Codec<JsonEvent>    TEXT_TO_JSON = textToJson();

    /**
     * Reads json and can handle both array and object type.
     */
    public static Codec<AnyJsonEvent> JSON_ANY     = json();

    /**
     * Reads json and this MUST be a json object and not an array.
     */
    public static Codec<JsonEvent>    JSON_OBJECT  = jsonObject();

    /**
     * Reads anything
     */
    public static Codec<BytesEvent>   BYTES        = bytes();


    private static Codec<AnyJsonEvent> json() {
       return new AbstractCodec<AnyJsonEvent>() {
           @Override
           public AnyJsonEvent from(ByteString b) {
               return jsonArray(b);
           }

           @Override
           public AnyJsonEvent from(Event event) {
               return from(event.raw())
                       .withMetaData(event);
           }

           @Override
           public String toString() {
               return "AnyJsonCodec";
           }
       };
    }

    private static Codec<JsonEvent> jsonObject() {
        return new AbstractCodec<JsonEvent>() {
            @Override
            public JsonEvent from(ByteString b) {
                return json(b);
            }

            @Override
            public JsonEvent from(Event event) {
                return from(event.raw()).withMetaData(event);
            }

            @Override
            public String toString() {
                return "JsonObjectCodec";
            }
        };
    }

    private static Codec<JsonEvent> textToJson() {
        return new AbstractCodec<JsonEvent>() {
            @Override
            public JsonEvent from(ByteString b) {
                return raw(b);
            }

            @Override
            public JsonEvent from(Event event) {
                return from(event.raw()).withMetaData(event);
            }

            @Override
            public String toString() {
                return "TextToJsonCodec";
            }
        };
    }

    private static Codec<BytesEvent> bytes() {
        return new AbstractCodec<BytesEvent>() {
            @Override
            public BytesEvent from(ByteString b) {
                return new BytesEvent(b);
            }

            @Override
            public BytesEvent from(Event event) {
                return from(event.raw()).withMetaData(event);
            }

            @Override
            public String toString() {
                return "BytesCodec";
            }
        };
    }



    private static AnyJsonEvent jsonArray(ByteString json) {
        try {

            JsonNode node = objectMapper.readTree(json.utf8());
            return new AnyJsonEvent(node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static JsonEvent raw(ByteString raw) {
        ObjectNode objectNode = objectMapper.createObjectNode()
                .put("message", raw.utf8())
                .put("@timestamp", now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        objectNode.set("tags",objectNode.arrayNode());
        return new JsonEvent(objectNode);
    }


    private static final JsonEvent json(ByteString json) {
        try {
            return new JsonEvent((ObjectNode)objectMapper.readTree(json.utf8()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
