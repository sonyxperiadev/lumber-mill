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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import lumbermill.internal.MapWrap;
import lumbermill.internal.elasticsearch.RequestSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AWSV4SignerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AWSV4SignerFactory.class);


    public static RequestSigner createAndAddSignerToConfig(MapWrap parameters) {

        AWSCredentialsProvider longLived = new DefaultAWSCredentialsProviderChain();
        AWSCredentialsProvider credentialsProvider;
        if (parameters.exists("role")) {
            LOG.info("Using IAM role {} to access Elasticsearch", parameters.asString("role"));
            credentialsProvider =  new STSAssumeRoleSessionCredentialsProvider(longLived, parameters.asString("role"), "lumbermill");
        } else {
            credentialsProvider =  longLived;
        }

        return new AWSV4SignerImpl(credentialsProvider,
                parameters.asString("region"),"es");


    }
}
