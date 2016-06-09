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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lumbermill.internal.MapWrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 *  Manages a KinesisProducer per stream to make configuration simpler.
 */
public class KinesisClientFactory {

    private static final String DEFAULT_PARTITION_KEY = "default";

    private Map<String, SimpleRetryableKinesisClient> clients = new HashMap<>();

    /**
     * Creates are retrieves an existing clients based on the parameters.
     */
    public synchronized SimpleRetryableKinesisClient getOrCreate(MapWrap parameters){
        if (clients.containsKey(parameters.asString("stream"))) {
            return clients.get(parameters.asString("stream"));
        }
        SimpleRetryableKinesisClient simpleRetryableKinesisClient = new SimpleRetryableKinesisClient(getAsyncClient(parameters),
                parameters.asString("stream"),
                //TODO: Consider forcing user to specify partition key
                parameters.get("partition_key", DEFAULT_PARTITION_KEY));
        clients.put(parameters.asString("stream"), simpleRetryableKinesisClient);
        return simpleRetryableKinesisClient;
    }

    private AmazonKinesisAsync getAsyncClient(MapWrap configuration) {

        Optional<ClientConfiguration> kinesisConfig = configuration.getIfExists("kinesis_config");
        if (kinesisConfig.isPresent()) {
            return createClient(kinesisConfig.get(), configuration);

        }

        return createClient(new ClientConfiguration()
                .withProxyHost("localhost")
                .withProxyPort(3128)
                .withMaxConnections(configuration.get("max_connections", 10))
                .withRequestTimeout(configuration.get("request_timeout", 60000)), configuration);
    }

    private AWSCredentialsProvider getAwsCredentialsProvider(MapWrap configuration, ClientConfiguration awsConfig) {
        AWSCredentialsProvider credentials = new DefaultAWSCredentialsProviderChain();
        Optional<String> roleArn = configuration.getIfExists("role_arn");
        if (roleArn.isPresent()) {
            credentials = new STSAssumeRoleSessionCredentialsProvider(credentials, roleArn.get(),
                    "lumbermill", awsConfig);
        }
        return credentials;
    }

    private AmazonKinesisAsync createClient(ClientConfiguration config,
                                             MapWrap configuration) {


        AmazonKinesisAsync kinesisClient = new AmazonKinesisAsyncClient(getAwsCredentialsProvider(
                configuration, config), config/*, Executors.newFixedThreadPool(config.getMaxConnections(),
                new ThreadFactoryBuilder().setNameFormat("lumbermill-async-kinesis-%d").build())*/);

        Regions region = Regions.fromName(configuration.get("region", "eu-west-1"));
        kinesisClient.setRegion(Region.getRegion(region));
        if (configuration.exists("endpoint")) {
            kinesisClient.setEndpoint(configuration.get("endpoint"));
        }

        return kinesisClient;
    }
}
