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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okio.ByteString;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Represents a Json Object(!)
 *
 * @see AnyJsonEvent for handling Json arrays
 */
public class JsonEvent extends MetaDataEvent {

    protected static ObjectMapper objectMapper = new ObjectMapper();

    protected final ObjectNode jsonNode;

    public JsonEvent() {
        this.jsonNode = objectMapper.createObjectNode();
    }


    public ObjectNode copyNode() {
        return jsonNode.deepCopy();
    }

    public JsonEvent(ObjectNode jsonNode) {
        this.jsonNode = jsonNode;
    }


    public JsonEvent put(String fieldName, String value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public JsonEvent put(String fieldName, int value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public JsonEvent put(String fieldName, long value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public JsonEvent put(String fieldName, double value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public JsonEvent put(String fieldName, float value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public JsonEvent put(String fieldName, boolean value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public JsonEvent put(String fieldName, BigDecimal value) {
        jsonNode.put(fieldName, value);
        return this;
    }

    public long asLong(String field) {
        return getNodeWithErrorIfNotExists(field).asLong();
    }

    public double asDouble(String field) {
        return getNodeWithErrorIfNotExists(field).asDouble();
    }

    public Boolean asBoolean(String field) {
        return getNodeWithErrorIfNotExists(field).asBoolean();
    }

    public JsonEvent putMetaData(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public ZonedDateTime timestamp(String field) {
        return ZonedDateTime.parse(valueAsString(field), DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    public ZonedDateTime timestamp() {
        return timestamp("@timestamp");
    }

    @Override
    public String valueAsString(String field) {

        Optional<JsonNode> nodeOptional = getNode(field);



        if (nodeOptional.isPresent()) {

            JsonNode node = nodeOptional.get();

            // TODO: This should find another home
            // Support for boolean expressions of arrays
            if (node instanceof ArrayNode) {
                Iterator<JsonNode> elements = node.elements();
                StringBuffer sb = new StringBuffer("[");
                while (elements.hasNext()) {
                    sb.append("'").append(elements.next().asText()).append("'");
                    if (elements.hasNext()) {
                        sb.append(",");
                    }
                }
                return sb.append("]").toString();
            }

            return node.asText();
        }

        return super.valueAsString(field);
    }


    public boolean has(String field) {
        return getNode(field).isPresent() ? true : super.has(field);
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(boolean pretty)
    {
        try {
            return pretty ? objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode)
                    : objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonEvent merge(JsonEvent event) {
        Iterator<String> stringIterator = event.jsonNode.fieldNames();
        while(stringIterator.hasNext()) {
            String field = stringIterator.next();
            jsonNode.set(field, event.jsonNode.get(field));
        }
        return this;
    }

    public void eachField(FieldProcessor processor) {

        ArrayList<String> list = new ArrayList<String >();
        Iterator<String> stringIterator = jsonNode.fieldNames();
        while(stringIterator.hasNext()) {
            list.add(stringIterator.next());
        }

        for(String field : list) {
            processor.process(field, jsonNode.get(field).asText());
        }
    }

    @Override
    public ByteString raw() {
        return ByteString.encodeUtf8(toString(false));
    }

    public JsonEvent remove(String... fields) {

        for(String field : fields) {
            if (jsonNode.has(field)) {
                jsonNode.remove(field);
            }
        }
        return this;
    }

    public boolean contains(String field, String value) {
        if (!jsonNode.has(field)) {
            return false;
        }
        JsonNode jsonNode = this.jsonNode.get(field);
        if (jsonNode instanceof ArrayNode) {
            for (JsonNode  node : jsonNode) {
                if (node.asText().equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    public JsonEvent add(String field, String... values) {
        for (String value : values) {
            if (jsonNode.has(field)) {
                ArrayNode arrayNode = (ArrayNode) this.jsonNode.get(field);
                arrayNode.add(value);
            } else {
                jsonNode.set(field, jsonNode.arrayNode().add(value));
            }
        }
        return this;
    }

    public JsonEvent rename(String from, String to) {
        if (jsonNode.has (from)) {
            jsonNode.set (to, jsonNode.get (from));
            jsonNode.remove (from);
        }
        return this;
    }

    public List<String> getTags() {

        if (!jsonNode.has ("tags")) {
           return Collections.EMPTY_LIST;
        }
        ArrayNode node = (ArrayNode)jsonNode.get ("tags");
        List<String> tags = new ArrayList<> ();
        for (JsonNode jNode : node) {
            tags.add (jNode.asText ());
        }
        return tags;
    }

    @Override
    public int hashCode() {
        return jsonNode.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonEvent jsonEvent = (JsonEvent) o;
        return jsonNode != null ? jsonNode.equals(jsonEvent.jsonNode) : jsonEvent.jsonNode == null;
    }

    @Override
    public void addTags(List<String> tags) {
        if (!jsonNode.has("tags")) {
            jsonNode.putArray("tags");
        }
        add("tags", tags.toArray(new String[0]));
    }

    @Override
    public Event addTag(String tag) {
        add("tags", tag);
        return this;
    }

    @Override
    public boolean hasTag(String tag) {
        return contains("tags", tag) ? true : super.hasTag(tag);
    }

    public void removeIfExists(String field) {
        if (has(field)) {
            remove(field);
        }
    }

    public JsonEvent add(Map<String, Object> fieldsAndValues) {
        for (String key : fieldsAndValues.keySet()) {
            putObject(key, fieldsAndValues.get(key));
        }
        return this;
    }

    private void putObject(String key, Object o) {
        if (o instanceof String) {
            jsonNode.put(key, (String) o);
        } else if (o instanceof Integer) {
            jsonNode.put(key, (Integer)o);
        } else if (o instanceof Boolean) {
            jsonNode.put(key, (Boolean)o);
        } else if (o instanceof Float) {
            jsonNode.put(key, (Float)o);
        } else {
            jsonNode.put(key, String.valueOf(o));
        }
    }

    private JsonNode getNodeWithErrorIfNotExists(String fieldOrPointer) {
        Optional<JsonNode> node = getNode(fieldOrPointer);
        if (!node.isPresent()) {
            throw new IllegalStateException("No value found for json field/pointer: " + fieldOrPointer);
        }
        return node.get();
    }

    private Optional<JsonNode> getNode(String nameOrJsonPointer)  {
        JsonNode node =  nameOrJsonPointer.startsWith("/") ? jsonNode.at(nameOrJsonPointer) : jsonNode.get(nameOrJsonPointer);
        return (node != null && !node.isMissingNode()) ? Optional.of(node) : Optional.empty();
    }

    public AnyJsonEvent child(String field) {
        return new AnyJsonEvent(this.jsonNode.get(field));
    }

    public JsonEvent objectChild(String field) {
        return new JsonEvent((ObjectNode) this.jsonNode.get(field));
    }

    /**
     * Lets expose this until we have a decent API to work with JSON.
     */
    public ObjectNode unsafe() {
        return this.jsonNode;
    }

    public interface FieldProcessor {
        void process(String field, String value);
    }
}
