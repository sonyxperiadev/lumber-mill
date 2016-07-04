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


import okio.ByteString;
import rx.Observable;

/**
 * An event is a single processing unit that passes the pipeline.
 */
public interface Event {

    boolean hasTag(String tag);

    /**
     * Checks if the event has the specified metadata or "other" field.
     * For bytes and text events, only metadata exists. For json events, this
     * method will first look in the json structure and if not found there it will
     * check its metadata.
     */
    boolean has(String field);

    <T extends Event> T put(String key, Object value);

    /**
     * Same as above, first checks the internal structure (if any) before checking metadata.
     */
    String valueAsString(String field);

    /**
     * The raw bytes of the event
     */
    ByteString raw();

    <T> T get(String key);

    <T extends Event> T addTag(String tag);

    <T extends Event> Observable<T> toObservable();
}
