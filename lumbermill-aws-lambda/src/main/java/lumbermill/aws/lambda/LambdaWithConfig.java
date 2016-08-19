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
package lumbermill.aws.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import lumbermill.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Utility class to read config values when using AWS Custom Resource LambdaWithConfig.
 * When using that custom resource, the configuration file will be a json file named "config.json"
 * and it is located in the root of the zip file.
 *
 * https://github.com/sonyxperiadev/amazon-custom-resources/tree/master/lambda-with-config
 */
@SuppressWarnings("unused")
public class LambdaWithConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(LambdaWithConfig.class);

    private static LambdaWithConfig lambdaWithConfig = new LambdaWithConfig("config.json");

    public static String readConfig(String key) {
        return readConfig(key, null);
    }

    public static String readConfig(String key, String defaultValue) {
        return lambdaWithConfig.getConfig(key, defaultValue);
    }

    // instance

    private final String configFile;
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();
    private JsonNode node;

    LambdaWithConfig(String configFile)  {
        this.configFile = configFile;
    }

    String getConfig(String key, String defaultValue) {
        return cache.computeIfAbsent(key, __ -> {
            String value = node().has(key) ?
                    node.get(key).asText():
                    defaultValue;
            if (value == null) {
                String msg = configFile + " is missing key: " + key;
                LOGGER.error(msg);
                throw new IllegalStateException(msg);
            } else {
                LOGGER.info("Reading key {}, default {} got value {}", key, defaultValue, value);
                return value;
            }
        });
    }

    private synchronized JsonNode node() {
        if (node == null) {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(configFile);

            if (resource == null) {
                throw new IllegalStateException("Failed to find file: " + configFile);
            }

            try {
                this.node = Json.OBJECT_MAPPER.readValue(new File(resource.getFile()), JsonNode.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            LOGGER.info("Loaded configuration file: {}", configFile);
        }
        return node;
    }

}
