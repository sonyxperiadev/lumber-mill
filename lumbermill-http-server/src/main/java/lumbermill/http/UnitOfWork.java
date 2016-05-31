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
import rx.Observable;
import rx.Subscriber;


/**
 * Represents the concept of a 'Unit-Of-Work' which means a bunch of data that should be
 * treated at a single 'transaction'. Basically, if something goes wrong during processing
 * the complete UnitOfWork can/will be canceled and perhaps reprocessed.
 *
 * Examples:
 *
 *  : Kinesis poll or Lambda event
 *  : Http request
 *  : S3 Poll or lambda event
 *  : SQS Event
 *
 * Not suitable:
 *
 *  : Redis pubsub since there is no ack function.
 *
 *
 * @param <T>
 */
public interface UnitOfWork<T> {

    Observable<T> observable();

    /**
     * Subscriber for this UnitOfWork that <b>MUST</b> be registered with the observable.
     */
    Subscriber subscriber();
}
