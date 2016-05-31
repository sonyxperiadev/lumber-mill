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
import rx.Observable.Transformer;

/**
 * Contract for building small reusable pipelines that you do need in more than one place
 * but you do not want to re-write all steps every time.
 *
 * It is also useful for decomposing you pipeline in multiple parts to make it easier
 * to test.
 *
 */
public interface EventProcessor<E extends Event> extends Transformer<E, E> {

    Observable<E> call(Observable<E> observable);
}
