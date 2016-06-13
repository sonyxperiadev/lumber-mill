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

import lumbermill.internal.MapWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class ElasticsearchClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchClientFactory.class);

    private final Map<String, ElasticSearchOkHttpClientImpl> cachedClients = new HashMap<>();

    public synchronized ElasticSearchOkHttpClientImpl ofParameters(MapWrap config) {
        config.assertExists("url", "type")
                .assertExistsAny("index", "index_prefix");
        return createClient(config);
    }

    private String cacheKey(String url, String index, boolean isPrefix) {
        return new StringBuilder().append(url).append(index).append(isPrefix).toString();
    }

    private ElasticSearchOkHttpClientImpl createClient(MapWrap config) {

        boolean isPrefix;
        String index;
        String url = config.asString("url");
        if (config.exists("index_prefix")) {
            isPrefix = true;
            index = config.asString("index_prefix");
        } else {
            isPrefix = false;
            index = config.asString("index");
        }

        String cacheKey = cacheKey(url, index, isPrefix);
        if (cachedClients.containsKey(cacheKey)) {
            LOGGER.trace("Using cached Elasticsearch client");
            return cachedClients.get(cacheKey);
        }
        LOGGER.trace("Creating new Elasticsearch client");
        final ElasticSearchOkHttpClientImpl es = new ElasticSearchOkHttpClientImpl (
                url,
                index,
                config.asString("type"),
                isPrefix);

        if (config.exists("document_id")) {
            es.withDocumentId(config.get("document_id"));
        }

        if (config.exists("signer")) {
            es.withSigner(config.get("signer"));
        }

        if (config.exists("timestamp_field")) {
            es.withTimestampField(config.asString("timestamp_field"));
        }
        cachedClients.put(cacheKey, es);

        return es;
    }
}
