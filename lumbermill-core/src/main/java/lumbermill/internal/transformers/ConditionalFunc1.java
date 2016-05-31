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
package lumbermill.internal.transformers;

import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import rx.functions.Func1;

import java.util.Map;

/**
 * Enables simple conditional statements in the pipeline
 */
public class ConditionalFunc1 {

    private final Condition condition;

    public ConditionalFunc1(Condition condition) {
        this.condition = condition;
    }

    public Func1<JsonEvent, JsonEvent> add(Map fieldsAndValues) {
        return jsonEvent -> {
            if (condition.match(jsonEvent)) {
                jsonEvent.add(fieldsAndValues);
            }
            return jsonEvent;
        };
    }

    public <E extends Event> Func1<E, E> map(Func1<E,E> f) {
        return e -> {
            if (condition.match(e)) {
                return f.call(e);
            }
            return e;
        };
    }

    public interface Condition {
        boolean match(Event event);
    }
}
