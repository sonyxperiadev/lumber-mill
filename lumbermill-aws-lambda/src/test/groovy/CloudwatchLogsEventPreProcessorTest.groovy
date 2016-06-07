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
import lumbermill.api.Codecs
import lumbermill.api.JsonEvent
import lumbermill.aws.lambda.CloudWatchLogsEventPreProcessor
import rx.Observable

import static org.assertj.core.api.Assertions.assertThat

class CloudwatchLogsEventPreProcessorTest extends GroovyTestCase {

    // This is the sample event from AWS Console, contains 2 events
    static String EVENT = '{\n' +
        '  "awslogs": {\n' +
        '    "data": "H4sIAAAAAAAAAHWPwQqCQBCGX0Xm7EFtK+smZBEUgXoLCdMhFtKV3akI8d0bLYmibvPPN3wz00CJxmQnTO41whwWQRIctmEcB6sQbFC3CjW3XW8kxpOpP+OC22d1Wml1qZkQGtoMsScxaczKN3plG8zlaHIta5KqWsozoTYw3/djzwhpLwivWFGHGpAFe7DL68JlBUk+l7KSN7tCOEJ4M3/qOI49vMHj+zCKdlFqLaU2ZHV2a4Ct/an0/ivdX8oYc1UVX860fQDQiMdxRQEAAA=="\n' +
        '  }\n' +
        '}'


    def void test_plain_text() {
        Observable.just(Codecs.JSON_OBJECT.from(EVENT))
            .compose(new CloudWatchLogsEventPreProcessor())
            .buffer(2)
            .doOnNext { List<JsonEvent> events ->
                assertThat(events.get(0).valueAsString('message')).isEqualTo("[ERROR] First test message")
                assertThat(events.get(1).valueAsString('message')).isEqualTo("[ERROR] Second test message")
                assertThat(events.get(0).valueAsString('logGroup')).isEqualTo("testLogGroup")
                assertThat(events.get(1).valueAsString('logGroup')).isEqualTo("testLogGroup")
                assertThat(events.get(0).valueAsString('logStream')).isEqualTo("testLogStream")
                assertThat(events.get(1).valueAsString('logStream')).isEqualTo("testLogStream")
                assertThat(events.get(0).valueAsString('@timestamp')).isEqualTo("2015-08-24T21:03:07+02:00")
                assertThat(events.get(1).valueAsString('@timestamp')).isEqualTo("2015-08-24T21:03:07.001+02:00")
            }
            .subscribe()
    }
}
