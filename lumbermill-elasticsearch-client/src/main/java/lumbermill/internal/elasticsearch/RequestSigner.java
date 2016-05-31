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
package lumbermill.internal.elasticsearch;

import lumbermill.api.JsonEvent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for supporting signed request for AWS Elasticsearch.
 * Use lumbermill-aws-elasticsearch to make use of this feature.
 */
public interface RequestSigner {

    void sign(SignableRequest request);

    interface SignableRequest {

        String uri();
        String method();
        Map<String, String> queryParams();
        Map<String, String> headers();
        Optional<byte[]> payload();
        List<JsonEvent> original();

        void addSignedHeaders(Map<String, String> headers);


    }
}

