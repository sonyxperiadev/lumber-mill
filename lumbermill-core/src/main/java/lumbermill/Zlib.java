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

import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.internal.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

class Zlib {

    private static final Logger LOGGER = LoggerFactory.getLogger(Zlib.class);

    public  <T extends Event>Func1<T, Observable<T>> compress() {
        return t -> Codecs.BYTES.from(Streams.zlibCompress(t.raw())).withMetaData(t).toObservable();
    }

    public  <T extends Event>Func1<T, Observable<T>> decompress() {
        return t -> Codecs.BYTES.from(Streams.zlibDecompress(t.raw())).withMetaData(t).toObservable();
    }

    public  <T extends Event>Func1<T, Observable<T>> tryDecompress(){
        return t -> {
            try {
                return Codecs.BYTES.from(Streams.zlibDecompress(t.raw())).withMetaData(t).toObservable();
            } catch (RuntimeException e) {
                LOGGER.trace("Could not decompress event, returning original");
                return Observable.just(t);
            }
        };
    }
}
