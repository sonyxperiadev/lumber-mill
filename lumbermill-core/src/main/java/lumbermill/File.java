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

import lumbermill.api.Codec;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.Streams;
import lumbermill.internal.StringTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


public class File {

    private static final Logger LOGGER = LoggerFactory.getLogger(File.class);

    File(){}

    /**
     * Reads each line of the file as an Event, specify codec to decide how to parse each line.
     * <pre> {@code flatMap (
     *     file (
     *         file : '/path/to/file.txt', // Supports templating '{path}'
     *         codec : Codecs.jsonObject() // Optional, default is Codecs.textToJson()
     * ))}</pre>
     */
    public <E extends Event> Func1<JsonEvent, Observable<E>> lines(Map map) {

        MapWrap config = MapWrap.of(map).assertExists("file");
        StringTemplate fileTemplate = config.asStringTemplate("file");
        Codec<E> codec = config.getObject("codec", (Codec<E>)Codecs.TEXT_TO_JSON);

        return event -> {
            String file = fileTemplate.format(event).get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Reading each line of file {} with codec {}", file, codec);
            }
            try (Stream<String> lines = Streams.lines(file)) {
                List<E> allLines = lines.map(line -> codec.from(line))
                        .collect(toList());
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Finished reading file {} with {} lines", file, allLines.size());
                }
                return Observable.from(allLines);
            }
        };
    }

    public <E extends Event> Observable<E> readFileAsLines(Map map) {
        return Observable.just(Codecs.TEXT_TO_JSON.from("{}")).flatMap(lines(map));
    }

    public <E extends Event> Observable<E> readFile(Map map) {
        MapWrap config = MapWrap.of(map).assertExists("file");
        String file = config.asString("file");
        Codec<E> codec = config.getObject("codec", (Codec<E>)Codecs.BYTES);
        return Observable.just(codec.from(Streams.read(new java.io.File(file))));
    }


}
