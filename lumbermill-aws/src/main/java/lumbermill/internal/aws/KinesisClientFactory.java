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
import lumbermill.api.Observables;
import lumbermill.internal.MapWrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

/**
 *  Manages a KinesisProducer per stream to make configuration simpler.
 */
public class KinesisClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(KinesisClientFactory.class);

    private Map<String, SimpleRetryableKinesisClient> clients = new HashMap<>();

    /**
     * Creates are retrieves an existing clients based on the parameters.
     */
    public synchronized SimpleRetryableKinesisClient getOrCreate(MapWrap parameters){
        if (clients.containsKey(parameters.asString("stream"))) {
            return clients.get(parameters.asString("stream"));
        }
        SimpleRetryableKinesisClient simpleRetryableKinesisClient =
                new SimpleRetryableKinesisClient(getAsyncClient(parameters),
                parameters.asString("stream"),
                //TODO: Consider forcing user to specify partition key
                parameters.getIfExists("partition_key"));
        clients.put(parameters.asString("stream"), simpleRetryableKinesisClient);

        if (parameters.exists("retry")) {
            MapWrap retryConfig = MapWrap.of(parameters.get("retry")).assertExists("policy");

            simpleRetryableKinesisClient.withRetryTimer(Observables.timer(retryConfig),
                    retryConfig.get("attempts", SimpleRetryableKinesisClient.DEFAULT_ATTEMPTS));
        }

        return simpleRetryableKinesisClient;
    }

    private AmazonKinesisAsync getAsyncClient(MapWrap configuration) {

        Optional<ClientConfiguration> kinesisConfig = configuration.getIfExists("kinesis_config");
        if (kinesisConfig.isPresent()) {
            return createClient(kinesisConfig.get(), configuration);

        }

        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withMaxConnections(configuration.get("max_connections", 10))
                .withRequestTimeout(configuration.get("request_timeout", 60000));
        if (System.getenv("https_proxy") != null) {
            URI proxy = URI.create(System.getenv("https_proxy"));
            LOGGER.info("Using proxy {}", proxy);
            clientConfiguration.withProxyHost(proxy.getHost())
                    .withProxyPort(proxy.getPort());
        }
        return createClient(clientConfiguration, configuration);
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
