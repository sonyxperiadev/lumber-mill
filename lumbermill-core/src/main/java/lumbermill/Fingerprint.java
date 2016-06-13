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

import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import lumbermill.api.Event;
import lumbermill.internal.StringTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func1;

/**
 * Used to create fingerprints (checksum/hashes) of parts of the contents in Events.
 */
class Fingerprint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Fingerprint.class);

    /**
     * Creates an MD5 hash based on the configured source string and stores it as
     * metadata under the field name 'fingerprint' (using same name as logstash).
     *
     * It is up to the user to create the source string to be used as fingerprint.
     * Best practice to separate each 'word' with a char, like a pipe (|) char to prevent
     * any unexpected behaviour. Read more at https://github.com/google/guava/wiki/HashingExplained.
     *
     * <pre>
     *
     * Groovy usage that creates a hash from two fields:
     *  {@code
     * fingerprint.md5 ('{message}|{@timestamp}')
     * }
     * </pre>
     */
    public static <E extends Event> Func1<E, E> md5(String sourcePattern) {
        StringTemplate template = StringTemplate.compile(sourcePattern);

        return e -> {
            String sourceValue = template.format(e).get();
            String hashAsHex = Hashing.md5().hashString(sourceValue, Charsets.UTF_8).toString();
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Fingerprint of {} => {}", sourceValue, hashAsHex);
            }
            // as Metadata
            return e.put("fingerprint", hashAsHex);
        };
    }

    /**
     * Creates an MD5 hash based on raw event, this is stored under metadata field name 'fingerprint' (using same name as logstash).
     *
     * <pre>
     *
     * Groovy usage that creates a hash from two fields:
     *  {@code
     * fingerprint.md5()
     * }
     * </pre>
     */
    public static <E extends Event> Func1<E, E> md5() {

        return e -> {
            String hashOfContents = Hashing.md5().hashString(e.raw().utf8(), Charsets.UTF_8).toString();
            // as Metadata
            return e.put("fingerprint", hashOfContents);
        };
    }
}
