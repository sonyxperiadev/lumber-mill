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
package lumbermill.internal.aws;

import lumbermill.internal.MapWrap;

import java.util.HashMap;
import java.util.Map;


public class S3ClientFactory {

    private static String ROLE_ARN_NONE = "none";

    private Map<String, S3ClientImpl> clients = new HashMap();

    public S3ClientImpl create(MapWrap parameters) {

        String roleArn = parameters.exists("roleArn")
                ? parameters.asString("roleArn")
                : ROLE_ARN_NONE;

        if (clients.containsKey(roleArn)) {
            return clients.get(roleArn);
        } else if (roleArn.equals(ROLE_ARN_NONE)) {
            S3ClientImpl s3Client = new S3ClientImpl();
            clients.put(ROLE_ARN_NONE, s3Client);
            return s3Client;
        } else {
            S3ClientImpl s3Client = new S3ClientImpl(roleArn);
            clients.put(roleArn, s3Client);
            return s3Client;
        }
    }
}
