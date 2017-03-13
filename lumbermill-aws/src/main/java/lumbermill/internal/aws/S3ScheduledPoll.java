/*
 * Copyright 2017 Sony Mobile Communications, Inc.
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lumbermill.S3;
import lumbermill.api.Codecs;
import lumbermill.api.JsonEvent;
import lumbermill.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;


/**
 * Periodically polls S3 for files and emits as observables.
 *
 * : Stateless, keeps it simple, remove or rename files to make them not be listed again.
 * : Single threaded
 * :
 */
public class S3ScheduledPoll implements S3.Poll {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ScheduledPoll.class);

    private final ProcessingState processingState = new InMemoryProcessingState();

    private final String bucket;
    private final String prefix;
    private final String suffix;
    private final Semaphore lock;

    private final TimeUnit notOlderThanTimeUnit = TimeUnit.MINUTES;
    private int notOlderThanValue;

    private AtomicReference<S3.UnitOfWorkListener> listener = new AtomicReference<>();

    private Set<S3ObjectSummary> s3ObjectsInPipeline = new HashSet<>();

    private  final AmazonS3 s3Client;

    public S3ScheduledPoll(AmazonS3 s3Client, String bucket, String prefix, String suffix, int max, int notOlderThan) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.prefix = prefix;
        this.suffix = suffix;
        this.lock = new Semaphore(max);
        this.notOlderThanValue = notOlderThan;
        doStart();
    }

    @Override
    public void onFile(S3.UnitOfWorkListener unitOfWorkListener) {
        listener.set(unitOfWorkListener);
    }

    private void doStart() {

        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("lumbermill-s3poll-%d").build())
                .scheduleWithFixedDelay(() -> {

                    if (this.listener.get() == null) {
                        LOGGER.info("No listener added, no action performed. Make sure you called onFile() method");
                        return;
                    }

                    if (lock.availablePermits() == 0) {
                        LOGGER.trace("No available permits");
                        return;
                    }

                    try {
                        LOGGER.trace("Polling files under s3://{}/{} available permits: {}", bucket, prefix, lock.availablePermits());

                        List<S3ObjectSummary> objectSummaries = list(Optional.empty());

                        if (objectSummaries.size() == 0) {
                            LOGGER.trace("No files found");
                            return;
                        }

                        objectSummaries.forEach(os -> subscribe(os, listener.get().apply(
                                Observable.just(os)
                                        .filter(summary -> lock.tryAcquire())
                                        .filter(s3ObjectsInPipeline::add)
                                        .filter(processingState::isUnprocessed)
                                        .map(this::toJsonEvent))));

                    } catch (RuntimeException ace) {
                        LOGGER.warn("Error listing S3 files: {}", ace.getMessage());
                    }
                }, 2, 10, TimeUnit.SECONDS);

    }

    private List<S3ObjectSummary> list(Optional<String> marker) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucket)
                .withPrefix(prefix);

        marker.ifPresent(listObjectsRequest::withMarker);

        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest);

        if (objectListing.getObjectSummaries().size() == 0) {
            return Lists.newArrayList();
        }

        List<S3ObjectSummary> collect = objectListing.getObjectSummaries().stream()
                .filter(summary -> summary.getLastModified().getTime() > (System.currentTimeMillis() - notOlderThanTimeUnit.toMillis(notOlderThanValue)))
                .filter(summary -> summary.getKey().endsWith(suffix))
                .collect(Collectors.toList());

        if (collect.size() > 0) {
            return collect;
        } else if (objectListing.getNextMarker() != null) {
            LOGGER.debug("Getting next page with marker {}", objectListing.getNextMarker());
            return list(Optional.of(objectListing.getNextMarker()));
        } else {
            return Lists.newArrayList();
        }
    }

    private void release() {
        LOGGER.trace("release()");
        lock.release();
    }

    private void subscribe(S3ObjectSummary os, Observable<? extends Object> result) {

        result.subscribe(new Subscriber<Object>() {
            @Override
            public void onCompleted() {
                if (s3ObjectsInPipeline.remove(os)) {
                    processingState.completed(os);
                    release();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (s3ObjectsInPipeline.remove(os)) {
                    release();
                }

                if (LOGGER.isTraceEnabled()) e.printStackTrace();
            }

            @Override
            public void onNext(Object event) { }
        });

    }

    private JsonEvent toJsonEvent(S3ObjectSummary summary) {
        try {
            return Codecs.JSON_OBJECT.from(Json.OBJECT_MAPPER.writeValueAsString(summary));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public interface ProcessingState {
        void completed(S3ObjectSummary s3ObjectSummary);
        boolean isUnprocessed(S3ObjectSummary summary);
    }

    private static class NoOptProcessingState implements ProcessingState {

        @Override
        public void completed(S3ObjectSummary s3ObjectSummary) {

        }

        @Override
        public boolean isUnprocessed(S3ObjectSummary summary) {
            return true;
        }
    }

    private static class InMemoryProcessingState implements ProcessingState {

        private final Set<String> processedItems = Sets.newHashSet();

        @Override
        public void completed(S3ObjectSummary s3ObjectSummary) {
            processedItems.add(s3ObjectSummary.getETag());
        }

        @Override
        public boolean isUnprocessed(S3ObjectSummary summary) {
            return !processedItems.contains(summary.getETag());
        }
    }
}
