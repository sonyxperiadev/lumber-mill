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
package lumbermill.aws.lambda

import lumbermill.api.Codecs
import lumbermill.api.Event
import lumbermill.api.EventProcessor
import lumbermill.api.JsonEvent
import rx.Observable

import static lumbermill.Core.*


/**
 * Reusable EventProcessor for decoding the contents an event received
 * from Cloud Watch Logs.
 *
 */
@SuppressWarnings("unused")
class CloudWatchLogsEventPreProcessor implements EventProcessor {

    Observable<Event> call(Observable observable) {
        // Read the actual data from json
        observable.map { JsonEvent jsonEvent ->
                return Codecs.BYTES.from (
                    jsonEvent.objectChild("awslogs").valueAsString("data"))
        }
        .flatMap ( base64.decode())
        .flatMap ( gzip.decompress())
        .flatMap ( toJsonObject())

        .flatMap { JsonEvent event ->

            // Denormalize, add logGroup and logStream to each event
            def logGroup = event.valueAsString('logGroup')
            def logStream = event.valueAsString('logStream')
            event.child('logEvents')
                .each()
                .map { JsonEvent jsonEvent ->
                    jsonEvent.put('logGroup',logGroup)
                        .put('logStream',logStream)
            }
        }
        .flatMap ( timestampFromMs('timestamp'))
        .flatMap ( remove('timestamp'))
    }
}