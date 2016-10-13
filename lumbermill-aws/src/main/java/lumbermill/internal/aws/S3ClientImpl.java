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
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lumbermill.api.Event;
import lumbermill.internal.Streams;
import lumbermill.internal.StringTemplate;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Optional;


/**
 * Important to NOT keep any state in member variables since this will be shared between (potentially) many
 * S3 outputs.
 */
public class S3ClientImpl<T extends Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ClientImpl.class);

    private AmazonS3 s3Client = null;

    private Optional<String> roleArn = Optional.empty();

    public S3ClientImpl() {
        init();
    }

    public S3ClientImpl(String roleArn) {
        this.roleArn = Optional.of(roleArn);
        init();
    }

    public void init() {
        ClientConfiguration awsConfig = new ClientConfiguration();
        if (System.getenv("https_proxy") != null) {
            URI proxy = URI.create(System.getenv("https_proxy"));
            awsConfig.setProxyHost(proxy.getHost());
            awsConfig.setProxyPort(proxy.getPort());
        }

        //awsConfig.setConnectionTimeout(2000);
        //awsConfig.setRequestTimeout(2000);
        //awsConfig.setSocketTimeout(2000);
        //awsConfig.setClientExecutionTimeout(2000);

        AWSCredentialsProvider credentials = new DefaultAWSCredentialsProviderChain();
        if (roleArn.isPresent()) {
            credentials = new STSAssumeRoleSessionCredentialsProvider(credentials, roleArn.get(),
                    "lumbermills3", awsConfig);
        }
        s3Client = new AmazonS3Client(credentials, awsConfig);

    }

    /**
     * Downloads a file from S3 defined by the specified event and bucket + key StringTemplates.
     *
     * It will return an Observable with same contents as incoming event and the location of the
     * downloaded file under fieldname "path". The downloaded file will be deleted when the pipeline
     * terminates and .
     *
     * @param removeFromS3 - the s3 file will be removed IF removeFromS3 is true and the pipeline succeeds, otherwise
     *                     it will be left.
     */
    public Observable<T> download(T event,
                                  StringTemplate bucketTemplate,
                                  StringTemplate keyTemplate,
                                  String outputFieldName,
                                  boolean removeFromS3) {
        String sBucket = format(event, bucketTemplate);
        String sKey    = format(event, keyTemplate);
        File file = this.get(sBucket, sKey);
        file.deleteOnExit();
        
        event.put(outputFieldName, file.getPath());
        LOGGER.debug("File download from s3 was stored in {}", file);

        return Observable.just(event)
                // Only delete from S3 if successful
                .doOnCompleted(() -> {
                    if (removeFromS3) {
                        this.delete(sBucket, sKey);
                        LOGGER.debug("Deleted s3 file s3://{}/{} successfully", sBucket, sKey);
                    }
                })
                .doOnUnsubscribe(() -> {
                    boolean deleted = file.delete();
                    LOGGER.debug("OnSubscribe:Deleted local file {} successfully ? {}", file, deleted);
                })
                .doOnTerminate(() -> {
                    boolean deleted = file.delete();
                    LOGGER.debug("OnTerminate:Deleted local file {} successfully ? {}", file, deleted);
                });
    }

    public void delete(String bucket,
                       String key) {
        LOGGER.debug("Deleting object s3://{}/{}", bucket, key);

        s3Client.deleteObject(bucket, key);
    }

    /**
     *
     */
    public void put(String file,
                    String bucket,
                    String key) {

        LOGGER.debug("Uploading to s3://{}/{}", bucket, key);

        s3Client.putObject(bucket, key, new File(file));
    }

    /**
     *
     */
    public void put(T event,
                    StringTemplate bucket,
                    StringTemplate key) {
        ObjectMetadata metadata = new ObjectMetadata();
        ByteString raw = event.raw();
        metadata.setContentLength(raw.size());

        String sKey = key.format(event).get();
        String sBucket = bucket.format(event).get();

        LOGGER.trace("Uploading to s3://{}/{} with size {}", sBucket, sKey, metadata.getContentLength());
        s3Client.putObject(sBucket, sKey,
                new ByteArrayInputStream(raw.toByteArray()), metadata);
    }


    public ByteString getAsBytes(String bucket, String key) {
        return Streams.read(get(bucket, key));
    }

    public File get(String bucket,
                 String key) {

        LOGGER.trace("Getting file from s3://{}/{}", bucket, key);
        File file;
        try {
            file = File.createTempFile("lumbermill", ".log");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            key = URLDecoder.decode(key, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        s3Client.getObject(new GetObjectRequest (
                bucket, key), file);
        return file;
    }

    public String format(T event, StringTemplate template) {
        return template.format(event).get();
    }
}
