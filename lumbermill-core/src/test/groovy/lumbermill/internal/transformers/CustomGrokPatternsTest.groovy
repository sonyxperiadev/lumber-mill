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
package lumbermill.internal.transformers

import lumbermill.internal.MapWrap
import org.junit.Test
import lumbermill.api.Codecs
import lumbermill.Core
import lumbermill.api.JsonEvent

import static Codecs.JSON_OBJECT
import static Codecs.TEXT_TO_JSON
import static org.assertj.core.api.Assertions.assertThat

/**
 * Tests that the custom parse patterns from patterns/aws works as expected
 */
class CustomGrokPatternsTest extends GroovyTestCase {

    static def ELB_ROW_EXPECTED_RESULT = '{\n' +
        '  "message" : "2016-03-11T13:55:51.847305Z prod-apig-LoadBala-W1C506EG6RQ4 216.137.32.245:43649 172.31.39.241:80 0.000044 1.212816 0.000041 400 400 145 25 \\"POST https://platform.lifelog.sonymobile.com:443/oauth/2/refresh_token?param1=value1&param2=value2 HTTP/1.1\\" \\"Apache-HttpClient/4.3.6 (java 1.5)\\" ECDHE-RSA-AES128-SHA TLSv1",\n' +
        '  "@timestamp" : "2016-03-11T13:55:51.847305Z",\n' +
        '  "tags" : [ ],\n' +
        '  "backend_ip" : "172.31.39.241",\n' +
        '  "backend_port" : 80,\n' +
        '  "backend_processing_time" : 1.212816,\n' +
        '  "backend_status_code" : 400,\n' +
        '  "client_ip" : "216.137.32.245",\n' +
        '  "client_port" : 43649,\n' +
        '  "elb_status_code" : 400,\n' +
        '  "httpversion" : "1.1",\n' +
        '  "loadbalancer" : "prod-apig-LoadBala-W1C506EG6RQ4",\n' +
        '  "received_bytes" : 145,\n' +
        '  "request" : "https://platform.lifelog.sonymobile.com:443/oauth/2/refresh_token?param1=value1&param2=value2",\n' +
        '  "request_processing_time" : 4.4E-5,\n' +
        '  "response_processing_time" : 4.1E-5,\n' +
        '  "sent_bytes" : 25,\n' +
        '  "useragent" : "Apache-HttpClient/4.3.6 (java 1.5)",\n' +
        '  "verb" : "POST"\n' +
        '}'

    static def ELB_ROW = '2016-03-11T13:55:51.847305Z prod-apig-LoadBala-W1C506EG6RQ4 216.137.32.245:43649 ' +
        '172.31.39.241:80 0.000044 1.212816 0.000041 400 400 145 25 "POST ' +
        'https://platform.lifelog.sonymobile.com:443/oauth/2/refresh_token?param1=value1&param2=value2 HTTP/1.1" ' +
        '"Apache-HttpClient/4.3.6 (java 1.5)" ECDHE-RSA-AES128-SHA TLSv1'


    static def LAMBDA_REPORT_ROW = '{\n' +
        '    "id": "32507763080146124827618319201546297167805252465098489864",\n' +
        '    "timestamp": 1457698511451,\n' +
        '    "message": "REPORT RequestId: e6da7858-e782-11e5-858c-f705fa25b607\\tDuration: 395.80 ms\\tBilled Duration: 400 ms \\tMemory Size: 1536 MB\\tMax Memory Used: 1468 MB\\t\",\n' +
        '    "@timestamp": "2016-03-11T12:15:11.451Z"\n' +
        '}'

    @Test
    def void test_lambda_request_report_pattern_json() {

        GrokFactory.create('message', '%{AWS_LAMBDA_REQUEST_REPORT}')
            .parse(JSON_OBJECT.from(LAMBDA_REPORT_ROW))
            .doOnNext({JsonEvent event ->
                assertThat(event.valueAsString('lambda_duration_ms')).isEqualTo('395.8')
                assertThat(event.valueAsString('lambda_billed_duration_ms')).isEqualTo('400.0')
                assertThat(event.valueAsString('lambda_memory_size_mb')).isEqualTo('1536')
                assertThat(event.valueAsString('lambda_memory_used_mb')).isEqualTo('1468')
        })
    }

    @Test
    def void test_elb_log() {

        println Codecs.TEXT_TO_JSON.from("hello".getBytes())
        GrokFactory.create('message', '%{AWS_ELB_LOG}')
            .parse(TEXT_TO_JSON.from(ELB_ROW))
            .map(Core.rename(MapWrap.of("from", "timestamp","to","@timestamp").toMap()))
            .doOnNext( { event ->
                assertThat(event.toString()).isEqualTo(ELB_ROW_EXPECTED_RESULT)
            } )
    }

}
