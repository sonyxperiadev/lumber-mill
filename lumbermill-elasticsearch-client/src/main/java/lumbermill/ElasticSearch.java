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
package lumbermill;

import lumbermill.api.JsonEvent;
import lumbermill.elasticsearch.ElasticSearchBulkRequestEvent;
import lumbermill.elasticsearch.ElasticSearchBulkResponseEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.elasticsearch.ElasticSearchOkHttpClientImpl;
import lumbermill.internal.elasticsearch.ElasticsearchClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@SuppressWarnings("unused")
public class ElasticSearch  {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearch.class);

    public static final ElasticSearch elasticsearch = new ElasticSearch();

    private ElasticsearchClientFactory clientFactory = new ElasticsearchClientFactory();

    public ElasticSearch() {}

    /**
     * Buffered client expecting a list of JsonEvents.
     *
     */
    public Func1<List<JsonEvent>, Observable<ElasticSearchBulkResponseEvent>> client(Map map) {
        final ElasticSearchOkHttpClientImpl client = clientFactory.ofParameters(MapWrap.of(map));
        return events -> client.post(events);
    }

    /**
     * Buffered client expecting a list of JsonEvents.
     *
     */
    public Func1<ElasticSearchBulkRequestEvent, Observable<ElasticSearchBulkResponseEvent>> bulkRequestClient(Map map) {
        final ElasticSearchOkHttpClientImpl client = clientFactory.ofParameters(MapWrap.of(map));
        return client::postEvent;
    }

    /**
     * Single event client.
     *
     */
    public Func1<JsonEvent, Observable<ElasticSearchBulkResponseEvent>> singleClient(Map map) {
        final ElasticSearchOkHttpClientImpl es = clientFactory.ofParameters(MapWrap.of(map));
        return jsonEvent -> es.post(Collections.singletonList(jsonEvent));
    }

    /**
     * Used for testing purposes
     */
    public Subscriber<JsonEvent> subscriber(Map map) {
        ElasticSearchOkHttpClientImpl es = clientFactory.ofParameters(MapWrap.of(map));
        return new Subscriber<JsonEvent>() {
            final List<JsonEvent> events = new ArrayList<>();

            @Override
            public void onCompleted() {
                LOGGER.trace("onCompleted, flushing events");
                es.post(events);
            }

            @Override
            public void onError(Throwable e){

            }

            @Override
            public void onNext(JsonEvent jsonEvent) {
                events.add(jsonEvent);
            }
        };
    }




}
