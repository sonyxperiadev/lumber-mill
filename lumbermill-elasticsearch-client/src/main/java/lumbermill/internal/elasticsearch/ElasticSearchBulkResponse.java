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
package lumbermill.internal.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.squareup.okhttp.Response;
import lumbermill.api.JsonEvent;
import lumbermill.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ElasticSearchBulkResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchBulkResponse.class);
    private final JsonNode responseContents;

    public final Map<JsonEvent, JsonEvent> eventWithResponse = new HashMap<>();
    public final List<JsonEvent> retryableEvents = new ArrayList<>();

    private ElasticSearchBulkResponse(JsonNode contents, RequestSigner.SignableRequest request) {
        this.responseContents = contents;
        getRetryableItems(request);
    }

    public static ElasticSearchBulkResponse parse(RequestSigner.SignableRequest request, Response response) {
        try {
            return new ElasticSearchBulkResponse(
                    Json.OBJECT_MAPPER.readValue(response.body().source().readByteString().utf8(),JsonNode.class),
                    request);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public boolean hasErrors() {
        return responseContents.get("errors").asBoolean();
    }

    List<JsonEvent> getRetryableItems(RequestSigner.SignableRequest request) {

        ArrayNode array = (ArrayNode)responseContents.get("items");
        int pos          = 0;
        int updatedCount = 0;

        for (JsonNode node: array) {
            JsonNode dataNode;

            if (node.has("create")) {
                dataNode = node.get("create");//.get("status").asInt();
             } else if (node.has("index")) {
                dataNode = node.get("index");//.get("status").asInt();
            } else {
                throw new IllegalStateException("Could not find field create or index in response");
            }
            int statusCode = dataNode.get("status").asInt();
            eventWithResponse.put(request.original().get(pos), new JsonEvent((ObjectNode)node));
            if (statusCode != 200 && statusCode != 201 && statusCode != 202) {
                // No idea to retry if BAD_REQUEST, just skip and log them
                String err = dataNode.has("error") ? dataNode.get("error").asText() : "no error message";
                if (statusCode == 400) {
                    LOGGER.info("Will not retry event due to BAD_REQUEST:" +  err);
                }

                LOGGER.debug("Bulk entry had errors, status: {}, error: {}", statusCode, err);
                retryableEvents.add(request.original().get(pos));
            } else {
                // Successful
                if (dataNode.get("_version").asInt() > 1) {
                    updatedCount++;
                }
            }
            pos++;
        }
        if (!retryableEvents.isEmpty()) {
            LOGGER.debug("Bulk response contains {} failed items out of {}", retryableEvents.size(), array.size());
        }
        if (updatedCount > 0) {
            LOGGER.debug("Bulk response contains {} document updates out of {}", updatedCount, array.size());
        }
        return retryableEvents;
    }
}
