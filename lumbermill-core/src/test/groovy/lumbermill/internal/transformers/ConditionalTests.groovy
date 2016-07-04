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

import lumbermill.api.BytesEvent
import lumbermill.api.Codecs
import lumbermill.api.JsonEvent
import org.junit.Test
import rx.Observable

import static lumbermill.Core.*
import static lumbermill.api.Codecs.TEXT_TO_JSON
import static org.assertj.core.api.Assertions.assertThat

class ConditionalTests extends GroovyTestCase {

    @Test
    def void test_if_absent() {

        Observable.just(TEXT_TO_JSON.from("hello world"))
            .flatMap(
                computeIfAbsent('asdfasdf') {
                    grok.parse (
                        field: 'message',
                        pattern: '%{WORD:word}'
                    )
                }
            )
            .doOnNext({ JsonEvent e -> assertThat(e.has("word")).isTrue()})
            .flatMap(
                computeIfAbsent('asdfasdf') { e ->
                    grok.parse (
                        field: 'word',
                        pattern: '%{WORD:word2}'
                    )(e)
                }
            )
            .doOnNext({ JsonEvent e -> assertThat(e.has("word2")).isTrue()})
            .flatMap(
                computeIfAbsent('asdfasdf') {
                    println('hello')
                }
            )
            .flatMap (
                computeIfAbsent('asdfasdf') {
                    toJsonObject()
                }
            )
            .flatMap (
                computeIfAbsent('asdfasdf') {
                    Codecs.BYTES.from("hello").toObservable()
                }
            )
            .doOnNext({BytesEvent e -> assertThat(e.raw().utf8()).isEqualTo("hello")})
            .doOnError({ e -> e.printStackTrace()})
            .toBlocking().subscribe()
    }

    @Test
    def void test_if_exists() {

        Observable.just(TEXT_TO_JSON.from("hello world"))
            .flatMap(
                computeIfExists('message') {
                    grok.parse (
                        field: 'message',
                        pattern: '%{WORD:word}'
                    )
                }
            )
            .doOnNext({ JsonEvent e -> assertThat(e.has("word")).isTrue()})
            .flatMap(
                computeIfExists('message') { e ->
                    grok.parse (
                        field: 'word',
                        pattern: '%{WORD:word2}'
                    )(e)
                }
            )
            .doOnNext({ JsonEvent e -> assertThat(e.has("word2")).isTrue()})
                .flatMap(
                    computeIfExists('message') {
                        println('hello')
                    }
            )
            .flatMap (
                computeIfExists('message') {
                    toJsonObject()
                }
            )
            .flatMap (
                computeIfExists('message') {
                    Codecs.BYTES.from("hello").toObservable()
                }
            )
            .doOnNext({BytesEvent e -> assertThat(e.raw().utf8()).isEqualTo("hello")})
            .toBlocking().subscribe()
    }

    @Test
    def void test_if_match() {

        Observable.just(TEXT_TO_JSON.from("hello world"))
            .flatMap(
            computeIfMatch('message', 'hello world') {
                grok.parse(
                    field: 'message',
                    pattern: '%{WORD:word}'
                )
            }
        ).doOnNext({ JsonEvent e -> assertThat(e.has("word")).isTrue() })
            .subscribe()
    }

    @Test
    def void test_if_not_match() {

        Observable.just(TEXT_TO_JSON.from("hello world"))
            .flatMap(
            computeIfNotMatch('message', 'hello apa') {
                grok.parse(
                    field: 'message',
                    pattern: '%{WORD:word}'
                )
            }
        ).doOnNext({ JsonEvent e -> assertThat(e.has("word")).isTrue() })
            .subscribe()
    }

    @Test
    def void test_if_has_tag() {

        Observable.just(TEXT_TO_JSON.from("hello world"))
            .flatMap (
                grok.parse(
                    field: 'message',
                    pattern: '%{NUMBER:word}'
                )
        ).flatMap(
            computeIfTagExists('_grokparsefailure') {
                addField("key", "value")
            }
        ).doOnNext({ JsonEvent e -> assertThat(e.has("key")).isTrue() })
            .subscribe()
    }

    @Test
    def void test_if_has_not_tag() {

        Observable.just(TEXT_TO_JSON.from("hello world"))
            .flatMap (
            grok.parse(
                field: 'message',
                pattern: '%{NUMBER:word}'
            )
        ).flatMap(
            computeIfTagAbsent('_grokparsefailure') {
                addField("key", "value")
            }
        ).doOnNext({ JsonEvent e -> assertThat(e.has("key")).isFalse() })
            .subscribe()
    }
}
