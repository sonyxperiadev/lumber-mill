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

import lumbermill.api.Event;
import lumbermill.internal.MapWrap;
import lumbermill.internal.aws.SimpleRetryableKinesisClient;
import lumbermill.internal.aws.KinesisClientFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;

/**
 * AWS Kinesis, currently only producer is supported.
 */
public class Kinesis<E extends Event> {

    private final KinesisClientFactory factory = new KinesisClientFactory();

    /**
     * Creates a buffered producer when using observable.buffer(size)
     */
    public Func1<List<E>, Observable<List<E>>> bufferedProducer(Map<String, Object> config) {
        final SimpleRetryableKinesisClient producer = factory.getOrCreate(MapWrap.of(config));
        return events -> producer.putRecords(events);
    }

    /**
     * Creates a single producer when processing each observable
     */
    public Func1<E, Observable<E>> producer(Map<String, Object> config) {
        final SimpleRetryableKinesisClient producer = factory.getOrCreate(MapWrap.of(config));
        return event -> producer.putRecord(event);

    }

 }
