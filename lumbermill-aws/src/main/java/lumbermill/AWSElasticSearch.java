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

import lumbermill.elasticsearch.ElasticSearchBulkResponseEvent;
import lumbermill.internal.MapWrap;
import lumbermill.api.JsonEvent;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;


import static lumbermill.ElasticSearch.elasticsearch;
import static lumbermill.internal.aws.AWSV4SignerFactory.createAndAddSignerToConfig;

/**
 * In order to use AWS Elasticsearch with role-based access control, this version provides
 * request signing to support this. If you are not using roled-based access control, you can
 * instead use lumbermill-elasticsearch-client only.
 */
public class AWSElasticSearch {


     public Subscriber<JsonEvent> subscriber(Map map) {
         MapWrap parameters = MapWrap.of(map);
         return elasticsearch.subscriber(parameters.put("signer", createAndAddSignerToConfig(parameters)).toMap());
     }

     public Func1<List<JsonEvent>, Observable<ElasticSearchBulkResponseEvent>> client(Map map) {
         MapWrap parameters = MapWrap.of(map);
         return elasticsearch.client(parameters.put("signer", createAndAddSignerToConfig(parameters)).toMap());
     }




}
