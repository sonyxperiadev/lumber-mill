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
package lumbermill.internal;

import org.junit.Test;

import static lumbermill.internal.BooleanExpression.fromString;

import static org.assertj.core.api.Assertions.assertThat;
import static lumbermill.api.Codecs.TEXT_TO_JSON;

public class BooleanExpressionTest {

    @Test
    public void testMultipleWithIntAndStringMatch() {

        BooleanExpression booleanExpression = fromString("{code} == 2 && " +
                "{code} < 10 && " +
                "'{message}' == 'test' &&" +
                "{code2} == 1.1");

        assertThat(booleanExpression.eval(
                TEXT_TO_JSON.from("test").put("code", 2).put("code2", 1.1f)))
                .isTrue();
    }

    @Test
    public void testExpressionWithNoFields() {

        BooleanExpression booleanExpression = fromString("2 == 2 && " +
                "'test' == 'test'");

        assertThat(booleanExpression.eval(
                TEXT_TO_JSON.from("test")))
                .isTrue();
    }

    @Test
    public void testExpressionWhenFieldNotFound() {

        BooleanExpression booleanExpression = fromString("{code2} == 2");

        assertThat(booleanExpression.eval(
                TEXT_TO_JSON.from("test")))
                .isFalse();
    }

    @Test
    public void testExpressionWithMilieuAndService() {

        BooleanExpression booleanExpression = fromString("'{milieu}' == 'prod' && '{serviceName}' == 'javaserver'");

        assertThat(booleanExpression.eval(
                TEXT_TO_JSON.from("test").put("milieu", "prod").put("serviceName", "javaserver")))
                .isTrue();

        assertThat(booleanExpression.eval(
                TEXT_TO_JSON.from("test").put("milieu", "unstable").put("serviceName", "javaserver")))
                .isFalse();

    }


    @Test
    public void testExpressionWithNonExistentArray() {
        BooleanExpression booleanExpression = fromString("{none}.contains('apa')");
        assertThat((booleanExpression.eval(TEXT_TO_JSON.from("pelle")))).isFalse();
    }

    /**
     * TODO: currently it fails if we try to do an array operation on a non array
     */
    @Test (expected = IllegalStateException.class)
    public void testExpressionWithNonArrayField() {
        BooleanExpression booleanExpression = fromString("{message}.contains('apa')");
        assertThat((booleanExpression.eval(TEXT_TO_JSON.from("pelle")))).isFalse();
    }


    @Test
    public void testExpressionWithEmptyArray() {
        BooleanExpression booleanExpression = fromString("{tags}.contains('apa')");
        assertThat((booleanExpression.eval(TEXT_TO_JSON.from("pelle")))).isFalse();
    }

    @Test
    public void testExpressionWithArray() {
        BooleanExpression booleanExpression = fromString("{tags}.contains('apa')");
        assertThat((booleanExpression.eval(TEXT_TO_JSON.from("pelle").add("tags", "apa","elefant")))).isTrue();
    }

    @Test
    public void testExpressionWithArrayAndParameterizedValue() {
        BooleanExpression booleanExpression = fromString("{tags}.contains('{tag}')");
        assertThat((booleanExpression.eval(TEXT_TO_JSON.from("pelle")
                .addTag("apa").addTag("elefant")
                .put("tag","apa")))).isTrue();
    }

    //TODO: Current check for null only works due to the fact that the expression will not be evaluated when
    //        the value is missing and always true when it exists

    @Test
    public void testValueExists() {
        assertThat(fromString("'{value}' != null").eval(TEXT_TO_JSON.from("pelle").put("value","value"))).isTrue();
    }

    @Test
    public void testValueDoesNotExist() {
        BooleanExpression booleanExpression = fromString("'{value}' != null");
        assertThat(booleanExpression.eval(TEXT_TO_JSON.from("pelle").put("key", "value"))).isFalse();
    }
}

