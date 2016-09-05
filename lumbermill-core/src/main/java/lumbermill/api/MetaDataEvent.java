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
package lumbermill.api;

import lumbermill.internal.MapWrap;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class MetaDataEvent implements Event {

    protected final MapWrap map = MapWrap.of(new HashMap() {
        {
            put("tags", new ArrayList<>());
        }
    });

    @Override
    public boolean hasTag(String tag) {
        List<String> tags = map.get("tags");
        return tags.contains(tag);
    }

    @Override
    public <T extends Event> T put(String key, Object value) {
        map.toMap().put(key, value);
        return (T)this;
    }

    @Override
    public boolean has(String field) {
        return map.exists(field);
    }

    @Override
    public String valueAsString(String field) {
        return has(field) ? map.asString(field) : null;
    }

    @Override
    public <T> T get(String key) {
        return (T)map.get(key);
    }

    public Event addTag(String tag) {
        List<String> tags = map.get("tags");
        tags.add(tag);
        return this;
    }

    public void addTags(List<String> tags) {
        List<String> tagss = map.get("tags");
        tagss.addAll(tags);
    }

    public <T extends Event> T withMetaData(Event mde) {
        if (mde instanceof MetaDataEvent) {
            MetaDataEvent e = (MetaDataEvent)mde;
            this.map.putAll(e.map);
        }
        return (T) this;
    }

    @Override
    public <T extends Event> Observable<T> toObservable() {
        return Observable.just((T)this);
    }
}

