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
 * Can contain any data
 */
public class BytesEvent extends MetaDataEvent {

    public final ByteString bytes;

    public BytesEvent(ByteString bytes) {
        this.bytes = bytes;
    }

    @Override
    public ByteString raw() {
        return bytes;
    }

    @Override
    public String toString() {
        return "BytesEvent{" +
                "bytes=" + bytes.toString() +
                '}';
    }
}
