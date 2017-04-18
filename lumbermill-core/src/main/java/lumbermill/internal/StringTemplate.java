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
import java.util.regex.Pattern;

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
    private final List<Field> fields = new ArrayList<>();

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
     * pattern. This will return Optional.empty() if a field is missing.
     */
    public Optional<String> format(Event event) {

        if (fields.size() > 0) {

            // For each Field, everything within {} will be replaced with the actual value but we start with
            // the raw complete pattern that was supplied
            String stringToReturn = pattern;

            for( Field field : fields) {

                boolean foundField = false;

                if (event.has(field.nameOrPointer())) {
                    stringToReturn = stringToReturn.replace (
                            String.format("{%s}", field.originalExpression()),
                            String.format("%s", event.valueAsString(field.nameOrPointer())));
                    foundField = true;
                    stringToReturn = field.valueOf(stringToReturn);

                } else {
                    String property = System.getProperty(field.nameOrPointer());
                    if (property != null) {
                        foundField = true;
                        stringToReturn = stringToReturn.replace (
                                String.format("{%s}", field.originalExpression()),
                                String.format("%s", property));
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Value for field {} was found as system property", field.nameOrPointer());
                        }
                    } else {
                        String env = System.getenv(field.nameOrPointer());
                        if (env != null) {
                            foundField = true;
                            stringToReturn = stringToReturn.replace(
                                    String.format("{%s}", field.originalExpression()),
                                    String.format("%s", env));
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Value for field {} was found as system environment variable", field.nameOrPointer());
                            }
                            continue;
                        }

                        if (field.hasDefault()) {
                            foundField = true;
                            stringToReturn = stringToReturn.replace(
                                    String.format("{%s}", field.originalExpression()),
                                    String.format("%s", field.defaultValue()));
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("Value for field {} was found as system environment variable", field.nameOrPointer());
                            }

                        }
                    }
                }

                if (!foundField) {
                    LOGGER.trace("Not all fields in pattern was found, returning an empty value");
                    return Optional.empty();
                }
            }

            return Optional.of(stringToReturn);

        } else {
            // No fields in pattern, return the pattern.
            return Optional.of(pattern);
        }
    }


    /**
     * Format that will only check System.getProperty() and System.getenv()
     */
    public Optional<String> format() {

        if (fields.size() == 0) {
            return Optional.of(pattern);
        }

        String valueToReturn = pattern;

        for( Field field : fields) {

            boolean foundField = false;

            String property = System.getProperty(field.nameOrPointer());
            if (property != null) {
                foundField = true;
                valueToReturn = valueToReturn.replace(
                        String.format("{%s}", field.originalExpression()),
                        String.format("%s", property));
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Value for field {} was found as system property", field.nameOrPointer());
                }
                continue;
            }

            String env = System.getenv(field.nameOrPointer());

            if (env != null) {
                foundField = true;
                valueToReturn = valueToReturn.replace(
                        String.format("{%s}", field.originalExpression()),
                        String.format("%s", env));
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Value for field {} was found as system environment variable", field.nameOrPointer());
                }
                continue;
            }

            if (!foundField && field.hasDefault()) {
                foundField = true;
                valueToReturn = valueToReturn.replace(
                        String.format("{%s}", field.originalExpression()),
                        String.format("%s", field.defaultValue()));
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Value for field {} was found as system environment variable", field.nameOrPointer());
                }

            }

            if (!foundField) {
                LOGGER.trace("Not all fields in pattern was found, returning an empty value");
                return Optional.empty();
            }

        }
        return Optional.of(valueToReturn);

    }

    private void initializeFields(String expression, List<Field> fields) {

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


    interface Field {
        boolean hasDefault();
        <T> String  valueOf(T value);


        /**
         * Used nameOrPointer, this is a single field value or a jsonPointer
         */
        String nameOrPointer();


        /**
         * Original nameOrPointer, used for replacement
         */
        String originalExpression();

        /**
         * The default value IF any
         */
        String defaultValue();


        boolean in(Event event);
    }

    private static class SimpleField implements Field {

        private final String expression;
        private final String name;
        private final String defaultValue;

        static Field of(String field) {
            return new SimpleField(field);
        }

        public String nameOrPointer() {
            return name;
        }

        public String originalExpression() {
            return expression;
        }

        public String defaultValue() {
            return defaultValue;
        }

        @Override
        public boolean in(Event event) {
            return event.has(name);
        }

        private SimpleField(String name) {
            this.expression = name;
            name = name.trim();

            if (name.contains("||")) {
                String[] split = name.split(Pattern.quote("||"));
                this.name = split[0].trim();
                this.defaultValue = split.length > 1 ? split[1].trim() : "";
            } else {
                this.name = name;
                this.defaultValue = null;
            }
        }

        public boolean hasDefault() {
            return defaultValue != null;
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
