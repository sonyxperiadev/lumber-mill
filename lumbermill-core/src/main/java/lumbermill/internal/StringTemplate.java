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

import lumbermill.api.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * StringTemplate is used instead of a reguar string in most configuration
 * options to provide the possibility to use variables from the event
 * instead of hard coded strings.
 *
 * StringTemplate.compile("{elb_status_code} == 500"}
 * StringTemplate.compile("'{host}' == '127.0.0.1'"}
 * StringTemplate.compile("application.status"} // Fields are not required
 *
 */
public class StringTemplate {

    private final Logger LOGGER = LoggerFactory.getLogger(StringTemplate.class);

    private final String pattern;
    private final List<SimpleField> fields = new ArrayList<>();

    public StringTemplate(String pattern) {
        this.pattern = pattern;
        initializeFields(pattern, fields);
    }

    public String original() {
        return pattern;
    }

    public static StringTemplate compile(String pattern) {
        if (!hasFields(pattern)) {
            return new NoFieldsTemplate(pattern);
        }
        return new StringTemplate(pattern);
    }

    private static boolean hasFields(String pattern) {
        return new StringTemplate(pattern).hasFields();
    }

    private boolean hasFields() {
        return fields.size() > 0;
    }

    /**
     * Formats the contents from the field according to the contents of the
     * pattern. This will return Optional.empty() if the pattern contains a field
     * that is not present in the event.
     */
    public Optional<String> format(Event event) {
        if (fields.size() > 0) {

            String newExpression = pattern;
            boolean foundField = false;
            for( SimpleField field : fields) {
                if (event.has(field.name)) {
                    newExpression = newExpression.replace(
                            String.format("{%s}", field.name),
                            String.format("%s", event.valueAsString(field.name)));
                    foundField = true;
                    newExpression = field.valueOf(newExpression);
                } else {
                    LOGGER.trace("Event has no field: {}", field.name);
                }
            }
            return foundField ?
                    Optional.of(newExpression) :
                    Optional.<String>empty();
        } else {
            return Optional.of(pattern);
        }
    }


    private void initializeFields(String expression, List<SimpleField> fields) {

        int first = expression.indexOf("{");
        int next = expression.indexOf("}");

        if( first != -1 && next != -1) {
            String substring = expression.substring(++first, next);
            fields.add(SimpleField.of(substring));
            initializeFields(expression.substring(++next, expression.length()), fields);
        }
    }

    @Override
    public String toString() {
        return "StringTemplate{" +
                "pattern='" + pattern + '\'' +
                ", fields=" + Arrays.toString(fields.toArray()) +
                '}';
    }

    private static class SimpleField {

        public final String name;

        static SimpleField of(String field) {
            return new SimpleField(field);
        }

        private SimpleField(String name) {
            this.name = name;
        }

        public <T> String  valueOf(T value) {
            return String.valueOf(value);
        }
    }

   private static class NoFieldsTemplate extends StringTemplate {

        private final Optional<String> pattern;
        public NoFieldsTemplate(String pattern) {
            super(pattern);
            this.pattern = Optional.of(pattern);
        }

        @Override
        public Optional<String> format(Event event) {
            return pattern;
        }
    }
}
