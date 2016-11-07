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
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.transformers.GrokFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;

public class Grok<E extends Event> {

    protected Grok() {}

    /**
     * Extracts values from a string and adds those as key/value pairs
     *
     * <pre> Groovy usage:
     *  {@code
     * parse (
     *     field : 'message',
     *     pattern : 'AWS_ELB_LOG',
     *     tagOnFailure : false
     * )
     * }</pre>
     */
    public Func1<JsonEvent, Observable<JsonEvent>> parse (Map conf) {
        MapWrap mapWrap = MapWrap.of(conf).assertExists("field", "pattern");
        lumbermill.internal.transformers.Grok grok = GrokFactory.create(mapWrap.asString("field"),
                mapWrap.asString("pattern"),
                mapWrap.get("tagOnFailure", true),
                mapWrap.get("tag", GrokFactory.ERROR_TAG));

        return t -> grok.parse(t);
    }

    public Func1<List<E>, Observable<List<E>>> parseBuffer(Map parameters) {
        MapWrap mapWrap = MapWrap.of(parameters).assertExists("field", "pattern");
        lumbermill.internal.transformers.Grok grok = GrokFactory.create(mapWrap.asString("field"),
                mapWrap.asString("pattern"),
                mapWrap.get("tagOnFailure", true),
                mapWrap.get("tag", GrokFactory.ERROR_TAG));
        return events -> grok.parse(events);
    }
}
