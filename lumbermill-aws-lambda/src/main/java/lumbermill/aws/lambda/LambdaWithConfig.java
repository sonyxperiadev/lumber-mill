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

    private JsonNode node;

    private static LambdaWithConfig lambdaWithConfig = new LambdaWithConfig();

    private LambdaWithConfig()  {

    }

    public static String readConfig(String key, String defaultValue) {

        String value = lambdaWithConfig.node().has(key) ?
                lambdaWithConfig.node.get(key).asText():
                defaultValue;
        LOGGER.info("Reading key {}, default {} got value {}", key, defaultValue, value);
        return value;
    }

    private synchronized JsonNode node() {
        if (node == null) {
            URL resource = Thread.currentThread().getContextClassLoader().getResource("config.json");

            if (resource == null) {
                throw new IllegalStateException("Failed to find config.json");
            }

            try {
                this.node = Json.OBJECT_MAPPER.readValue(new File(resource.getFile()), JsonNode.class);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return node;
    }

    public static String readConfig(String key) {
        return readConfig(key, null);
    }
}
