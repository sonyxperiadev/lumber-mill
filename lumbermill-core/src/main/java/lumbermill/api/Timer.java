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

import rx.Observable;

/**
 * Timers wraps HOW a delay is calculated
 */
public interface Timer {

    /**
     * Returns an observable that will emit the 'next' timer value
     * @return  next value
     */
    Observable<Long> next();

    int attempt();

     interface Factory {

         /**
          * Factory method for creating a new instance when the timer should be reset
          * @return a new instance
          */
         Timer create();

     }
}
