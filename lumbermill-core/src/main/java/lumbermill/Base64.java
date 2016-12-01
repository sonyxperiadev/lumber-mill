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

import lumbermill.api.BytesEvent;
import lumbermill.api.Event;
import okio.ByteString;
import rx.Observable;
import rx.functions.Func1;

/**
 * Base64 encode and decode, accessible from Code.base64
 */
public class Base64 {

    public <T extends Event> Func1<T, Observable<T>> encode() {
        return t -> new BytesEvent(ByteString.decodeBase64(t.raw().base64())).toObservable();
    }

    public <T extends Event> Func1<T,Observable<T>> decode() {
        return t -> new BytesEvent(ByteString.decodeBase64(t.raw().utf8())).toObservable();
    }

}
