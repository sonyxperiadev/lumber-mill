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
import java.util.Optional;


public class IndexFailedException extends RuntimeException {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexFailedException.class);

    private final Optional<Response> response;

    public IndexFailedException(Throwable t) {
        super(t);
        response = Optional.empty();
    }

    private IndexFailedException(Response response, String msg) {
        super(msg);
        this.response = Optional.of(response);
    }

    public static IndexFailedException of(Response response) {
        try {
            return new IndexFailedException(response, response.code() + ", message:" + response.message() +
                    ", body: " + response.body().string());
        } catch (IOException e) {
            LOGGER.warn("Failed to extract body from error message: " + e.getMessage());
            return new IndexFailedException(response, response.code() + ", message:" + response.message());
        }
    }

    public static IndexFailedException ofIOException(IOException e) {
        return new IndexFailedException(e);
    }

    public boolean isConnectFailure() {
        return !response.isPresent();
    }

    public int statusCode() {
        return !isConnectFailure() ? response.get().code() : 0;
    }
}
