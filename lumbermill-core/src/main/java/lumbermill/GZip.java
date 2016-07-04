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
import lumbermill.internal.MapWrap;
import lumbermill.internal.Streams;
import lumbermill.internal.StringTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.io.File;
import java.util.Map;

/**
 * GZip compression, accessible from Core.gzip
 */
class GZip {

    private static final Logger LOGGER = LoggerFactory.getLogger(GZip.class);

    private final static String DEFAULT_COMPRESS_OUTPUT_FIELD = "gzip_path_compressed";
    private final static String DEFAULT_DECOMPRESS_OUTPUT_FIELD = "gzip_path_decompressed";

    public  <T extends Event>Func1<T, Observable<T>> compress(Map map) {

        MapWrap parameters = MapWrap.of(map)
                .assertExists("file");
        final StringTemplate template = parameters.asStringTemplate("file");
        final String outputField = parameters.get("output_field", DEFAULT_COMPRESS_OUTPUT_FIELD);

        return event -> {
            File compressed = Streams.gzip(new File(template.format(event).get()));
            event.put(outputField, compressed.getPath());
            return  Observable.just(event)
                    .doOnTerminate(() -> {
                        boolean deleted = compressed.delete();
                        LOGGER.debug("Deleted file {} successfully ? {}", compressed, deleted);
                    });
        };
    }

    public  <T extends Event>Func1<T, Observable<T>> decompress(Map map) {

        MapWrap parameters = MapWrap.of(map)
                .assertExists("file");
        final StringTemplate template = parameters.asStringTemplate("file");
        final String outputField = parameters.get("output_field", DEFAULT_DECOMPRESS_OUTPUT_FIELD);

        return event -> {
            File decompressed = Streams.gunzip(new File(template.format(event).get()));
            event.put(outputField, decompressed.getPath());
            return  Observable.just(event)
                    .doOnTerminate(() -> {
                        boolean deleted = decompressed.delete();
                        LOGGER.debug("Deleted file {} successfully ? {}", decompressed, deleted);
                    });
        };
    }


    public  <T extends Event>Func1<T, Observable<T>> compress() {
        return t -> {
            LOGGER.debug("Compressing event");
            return  Codecs.BYTES.from(Streams.gzip(t.raw())).withMetaData(t).toObservable();
        };
    }

    public  <T extends Event>Func1<T, Observable<T>> decompress() {
        return t -> {
            LOGGER.debug("decompressing event");
            return Codecs.BYTES.from(Streams.gunzip(t.raw())).withMetaData(t).toObservable();
        };
    }
}
