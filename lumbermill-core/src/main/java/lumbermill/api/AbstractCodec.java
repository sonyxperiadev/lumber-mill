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


/**
 * Default Codec that defers actual creation to single method
 */
public abstract class AbstractCodec<T extends Event> implements Codec<T> {


    @Override
    public T from(byte[] b) {
        return from(ByteString.of(b));
    }

    @Override
    public T from(String s) {
        return from(ByteString.encodeUtf8(s));
    }
}
