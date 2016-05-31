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

import groovy.lang.Tuple2;
import lumbermill.api.JsonEvent;
import lumbermill.api.MetaDataEvent;
import okio.ByteString;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;


public class ElasticSearchBulkRequestEvent extends MetaDataEvent implements Iterable<JsonEvent> {

    final List<JsonEvent> events;

    public ElasticSearchBulkRequestEvent(List<JsonEvent> events) {
        this.events = events;
    }

    @Override
    public ByteString raw() {
        return ByteString.encodeUtf8(events.stream()
                .map(e -> e.toString(false))
                .reduce((s1, s2) -> s1 + "\n" + s2)
                .get());
    }

    @Override
    public String toString() {
        return raw().toString();
    }

    @Override
    public Iterator<JsonEvent> iterator() {
        return events.iterator();
    }

    public List<Tuple2<JsonEvent, JsonEvent>> indexRequests() {
        System.out.println("Size: " + events.size());
        List<Tuple2<JsonEvent, JsonEvent>> indexEvents = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            indexEvents.add(new Tuple2<>(events.get(i), events.get(i+1)));
            i++;
        }
        System.out.println("Return size: " + indexEvents.size());
        return indexEvents;
    }

    public Stream<JsonEvent> stream() {
        return events.stream();
    }
}
