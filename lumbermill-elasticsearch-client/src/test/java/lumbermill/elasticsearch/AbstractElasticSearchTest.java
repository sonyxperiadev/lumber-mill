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

import com.google.common.collect.Lists;
import lumbermill.api.Codecs;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.elasticsearch.ElasticSearchOkHttpClientImpl;
import lumbermill.internal.elasticsearch.ElasticsearchClientFactory;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Simple base class for testing Bulk client integration with a "real" elasticsearch backend.
 * For each created client,  a new index + type is created which separates each test, so we do not have do start and stop
 * elasticsearch between each call.
 */
public abstract class AbstractElasticSearchTest {

    private static final String HTTP_PORT = "9205";
    private static final String HTTP_TRANSPORT_PORT = "9305";
    private static final String ES_WORKING_DIR = "build/es";

    static String clusterName = "lumbermill-test";

    protected static Node node;

    private String index;
    private String type;

    @Before
    public void prepare() {
        index = UUID.randomUUID().toString() + "-";
        type = UUID.randomUUID().toString();
    }


    @BeforeClass
    public static void startElasticsearch() throws Exception {
        removeOldDataDir(ES_WORKING_DIR + "/" + clusterName);

        Settings settings = ImmutableSettings.builder()
                .put("path.home", ES_WORKING_DIR)
                .put("path.conf", ES_WORKING_DIR)
                .put("path.data", ES_WORKING_DIR)
                .put("path.work", ES_WORKING_DIR)
                .put("path.logs", ES_WORKING_DIR)
                .put("http.port", HTTP_PORT)
                .put("transport.tcp.port", HTTP_TRANSPORT_PORT)
                .put("index.number_of_shards", "1")
                .put("index.number_of_replicas", "0")
                .put("discovery.zen.ping.multicast.enabled", "false")
                .build();
        node = nodeBuilder().settings(settings).clusterName(clusterName).client(false).node();
        node.start();

    }

    @AfterClass
    public static void stopElasticsearch() throws Exception {
        node.close();
        removeOldDataDir(ES_WORKING_DIR + "/" + clusterName);
    }

    private static void removeOldDataDir(String datadir) throws Exception {
        File dataDir = new File(datadir);
        if (dataDir.exists()) {
            boolean deleted = FileSystemUtils.deleteRecursively(dataDir);
            System.out.println("Was deleted ? " + deleted);
        } else {
            System.out.println("DIR does not exist " + dataDir.getAbsolutePath());
        }
    }

    protected SearchResponseWrapper findAll() {
        return SearchResponseWrapper.of(node.client().prepareSearch().setTypes(type)
                .execute().actionGet());
    }

    // IF es is queried directly after http bulk post we get 0 result, this approach solves
    // this issue.
    protected Callable<Boolean> hitCountIs(int hitCount) {
        return () -> findAll().response.getHits().totalHits() == hitCount;
    }

    protected List<JsonEvent> simpleEventsOfSize(int count,boolean assignUuid) {

        ArrayList<JsonEvent> events = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            JsonEvent from = Codecs.TEXT_TO_JSON.from("Hello mighty mouse");
            if (assignUuid) {
                from.put("uuid",  UUID.randomUUID().getLeastSignificantBits());
            }
            events.add(from);
        }
        return events;
    }

    protected List<JsonEvent> simpleEventsOfSizeAndRandomField(int count, String fieldName) {
        ArrayList<JsonEvent> events = Lists.newArrayList();
        for (int i = 0; i < count; i++) {
            events.add(Codecs.TEXT_TO_JSON.from("Hello mighty mouse").put(fieldName, UUID.randomUUID().toString()));
        }
        return events;
    }


    protected ElasticSearchOkHttpClientImpl bulkClient(MapWrap extraParameters) {

        MapWrap config = MapWrap.of(
                "url", "http://localhost:9205",
                "type", type);
        config.putAll(extraParameters);

        if (!extraParameters.exists("index") && !extraParameters.exists("index_prefix")) {
            // Default but can be overriden
            config.put("index_prefix", index);
        }

        return new ElasticsearchClientFactory().ofParameters(config);
    }


    protected ElasticSearchOkHttpClientImpl bulkClient() {
        return bulkClient(MapWrap.of(new HashMap<>()));
    }
}

