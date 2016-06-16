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
package lumbermill.elasticsearch;

import com.squareup.okhttp.Response;
import lumbermill.internal.elasticsearch.ElasticSearchOkHttpClientImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class IndexFailedException extends RuntimeException {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexFailedException.class);

    public IndexFailedException(Throwable t) {
        super(t);
    }

    private IndexFailedException(String msg) {
        super(msg);
    }

    public static IndexFailedException of(Response response) {
        try {
            return new IndexFailedException(response.code() + ", message:" + response.message() +
                    ", body: " + response.body().string());
        } catch (IOException e) {
            LOGGER.warn("Failed to extract body from error message: " + e.getMessage());
            return new IndexFailedException(response.code() + ", message:" + response.message());
        }
    }
}
