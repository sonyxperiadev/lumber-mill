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
package lumbermill.http;


import lumbermill.api.Event;

/**
 *
 * Implement this interface for sources that want to provide a subscriber that can be called on success
 * or onError. This means that the source can wait to "commit" or remove etc until the the data has been
 * processed.
 *
 * An example would be http where we do not want to signal to clients until we now what happened.
 */
public interface UnitOfWorkAwareSource<T extends Event> {

    /**
     * Use tags to decide which type of request to subscribe to instead of using on() to get all.
     * NOTE: Only <b>ONE</b> on*() method will be invoked for each UnitOfWork
     */
    UnitOfWorkAwareSource<T> onTag(String tag, UnitOfWorkListener callback);

    /**
     * Receives all events that are not picked up by onTag() method.
     */
    UnitOfWorkAwareSource<T> on(UnitOfWorkListener callback);


}
