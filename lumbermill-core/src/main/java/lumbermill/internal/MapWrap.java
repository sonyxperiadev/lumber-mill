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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Groovy maps are used to make the API simple to use and this class
 * is used when accessing this maps from Java.
 *
 * It also uses some methods from guava ImmutableMap so we do not have to depend
 * on Guava.
 */
@SuppressWarnings("unused")
public final class MapWrap {

    private final static Logger LOGGER = LoggerFactory.getLogger(MapWrap.class);

    private final Map<String, Object> config;

    public static MapWrap of(Map map) {
        return new MapWrap(map);
    }

    private MapWrap(Map map) {
        this.config = new HashMap<>(map);
    }

    public Map toMap() {
        return config;
    }

    public String asString(String field) {
        return String.valueOf(config.get(field));
    }

    public int asInt(String field) {
       return asNumber(field).intValue();
    }

    public long asLong(String field) {
       return asNumber(field).longValue();
    }

    public float asFloat(String field) {
       return asNumber(field).floatValue();
    }

    private Number asNumber(String field) {
        Object value = getRaw(field);
        if (value instanceof Number) {
            return (Number)value;
        }
        return Double.parseDouble(field);
    }

    public MapWrap putAll(MapWrap mapWrap) {
        this.config.putAll(mapWrap.config);
        return this;
    }

    public MapWrap put(String key, Object value) {
        this.config.put(key, value);
        return this;
    }

    public boolean asBoolean(String field) {
        Object value = getRaw(field);
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    public <T> T get(String field) {
        return (T) config.get(field);
    }

    public <T> Optional<T> getIfExists(String field) {
        if (exists(field)) {
            return Optional.of(get(field));
        }
        return Optional.empty();
    }

    public boolean exists(String field) {
        return config.containsKey(field);
    }

    private <T> T getRaw(String field) {
        if (!exists(field)) {
            throw new IllegalStateException(format("The field %s does not exist in configuration " +
                    "(and might be required), keys are %s", field,
                    Arrays.toString(config.keySet().toArray())));
        }
        return (T) config.get(field);
    }

    @Override
    public String toString() {
        return "MapWrap{" +
                "config=" + config +
                '}';
    }

    public StringTemplate asStringTemplate(String name) {
        return StringTemplate.compile(asString(name));
    }

    public MapWrap assertExists(String... fields) {
        for( String s: fields) {
            if (! exists(s)) {
                throw new IllegalStateException(format("The field \'{}\' does not exist in configuration " +
                        "and is required, existing keys are " +
                        Arrays.toString(config.keySet().toArray())));
            }
        }
        return this;
    }

    public MapWrap assertExistsAny(String... fields) {
        for( String s: fields) {
            if ( exists(s)) {
                return this;
            }
        }
        throw new IllegalStateException(format("One of the fields \'{}\' must exist in configuration " +
                "but does not, existing keys are ",Arrays.toString(fields),
                Arrays.toString(config.keySet().toArray())));
    }


    public <T> T get(String field, T defauLt) {
        if (exists(field)) {
            return get(field);
        }
        LOGGER.debug("No configured value found for key {}, using default value {}",
                field, String.valueOf(defauLt));
        return defauLt;
    }

    public static <K,V> MapWrap of (String k1, V v1) {
        return new MapWrap(new HashMap<>()).put(k1, v1);
    }

    public static <K,V> MapWrap of (String k1, V v1, String k2, V v2) {
        return new MapWrap(new HashMap<>()).put(k1, v1).put(k2, v2);
    }

    public static <K,V> MapWrap of (String k1, V v1, String k2, V v2, String k3, V v3) {
        return new MapWrap(new HashMap<>()).put(k1, v1).put(k2, v2).put(k3, v3);
    }

    public static <K,V> MapWrap of (String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4) {
        return new MapWrap(new HashMap<>()).put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4);
    }

    public static <K,V> MapWrap of (String k1, V v1, String k2, V v2, String k3, V v3, String k4, V v4, String k5, V v5) {
        return new MapWrap(new HashMap<>()).put(k1, v1).put(k2, v2).put(k3, v3).put(k4, v4).put(k5, v5);
    }



}
