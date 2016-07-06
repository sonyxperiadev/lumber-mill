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
package lumbermill.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lumbermill.api.JsonEvent;
import lumbermill.internal.Json;
import lumbermill.internal.elasticsearch.ElasticSearchBulkResponse;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * <p>This result matches an Elasticsearch Bulk API response
 */
public class ElasticSearchBulkResponseEvent extends JsonEvent {

    private final Map<JsonEvent, JsonEvent> eventAndResponse = new HashMap<>();

    public static ElasticSearchBulkResponseEvent of(ElasticSearchBulkResponse bulkResponse) {
        return new ElasticSearchBulkResponseEvent(buildJsonResponse(bulkResponse.hasErrors(), bulkResponse.eventWithResponse),
                bulkResponse.eventWithResponse);
    }

    public static ElasticSearchBulkResponseEvent ofPostponed(List<JsonEvent> events) {
        return new ElasticSearchBulkResponseEvent(buildPostponedJsonResponse(events), new HashMap<>());
    }

    public static ElasticSearchBulkResponseEvent ofPostponed(ElasticSearchBulkRequestEvent requestEvent) {
        return new ElasticSearchBulkResponseEvent(buildPostponedJsonResponse(requestEvent), new HashMap<>());
    }


    private ElasticSearchBulkResponseEvent(ObjectNode node, Map<JsonEvent, JsonEvent> eventAndResponse) {
        super(node);
        this.eventAndResponse.putAll(eventAndResponse);
    }


    public ElasticSearchBulkResponseEvent nextAttempt(ElasticSearchBulkResponse elasticSearchBulkResponse) {
        eventAndResponse.putAll(elasticSearchBulkResponse.eventWithResponse);
        return new ElasticSearchBulkResponseEvent(buildJsonResponse(elasticSearchBulkResponse.hasErrors(), eventAndResponse),
                eventAndResponse);
    }

    public Observable<JsonEvent> arguments() {
        return Observable.from(eventAndResponse.keySet());
    }

    private static ObjectNode buildJsonResponse(boolean hasErrors, Map<JsonEvent, JsonEvent> eventAndResponse) {
        ObjectNode node = Json.OBJECT_MAPPER.createObjectNode()
                .put("errors", hasErrors)
                .put("took", 1L);
        node.putArray("items")
                .addAll(
                        eventAndResponse.values()
                                .stream()
                                .map(jsonEvent -> jsonEvent.copyNode())
                                .collect(Collectors.toList()));
        return node;

    }


    private static ObjectNode buildPostponedJsonResponse(ElasticSearchBulkRequestEvent requestEvent) {
        ObjectNode node = Json.OBJECT_MAPPER.createObjectNode()
                .put("errors", false)
                .put("took", 1L);
        node.putArray("items")
                .addAll(
                        requestEvent.indexRequests().stream()
                                .map(tuple -> tuple.getSecond())
                                .map(ElasticSearchBulkResponseEvent::toPostponsedEvent)
                                .collect(Collectors.toList()));
        return node;
    }

    private static ObjectNode buildPostponedJsonResponse(List<JsonEvent> events) {
        ObjectNode node = Json.OBJECT_MAPPER.createObjectNode()
                .put("errors", false)
                .put("took", 1L);
        node.putArray("items")
                .addAll(
                        events.stream()
                                .map(ElasticSearchBulkResponseEvent::toPostponsedEvent)
                                .collect(Collectors.toList()));
        System.out.println("Response built: " + new JsonEvent(node).toString(true));
        return node;

    }

    private static JsonNode toPostponsedEvent(JsonEvent jsonEvent) {
        ObjectNode objectNode = Json.OBJECT_MAPPER.createObjectNode();
        objectNode.putObject("create").put("_id", "ID_CREATION_POSTPONED").put("status", 202);
        return objectNode;
    }

    /*
      Below are used for test and debugging
     */
    public List<String> indexNames() {
        ArrayNode array = (ArrayNode) super.jsonNode.get("items");
        List<String> indices = new ArrayList<>();
        array.forEach(created -> indices.add(dataNode(created).get("_index").asText()));
        return indices;
    }

    public List<String> types() {
        ArrayNode array = (ArrayNode) super.jsonNode.get("items");
        List<String> indices = new ArrayList<>();
        array.forEach(created -> indices.add(created.fieldNames().next()));
        return indices;
    }

    public List<String> versions() {
        ArrayNode array = (ArrayNode) super.jsonNode.get("items");
        List<String> indices = new ArrayList<>();
        array.forEach(created -> indices.add(dataNode(created).get("_version").asText()));
        return indices;
    }

    public int count() {
        return super.jsonNode.get("items").size();
    }

    /**
     * Elasticsearch returns "create" if an entry was created and "index" if it was updated.
     */
    private JsonNode dataNode(JsonNode node) {
        String action = node.has("index") ? "index" : "create";
        return node.get(action);
    }
}
