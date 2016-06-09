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

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;
import com.amazonaws.services.kinesis.model.PutRecordsResultEntry;
import lumbermill.api.Event;
import lumbermill.aws.FatalAWSException;
import lumbermill.internal.StringTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.ReplaySubject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;

/**
 * Simple wrapper around AmazonKinesisClient that converts Events to Records.
 * It will do its best to retry if there are failed records.
 *
 * TODO: Handle too large items, today a large item will prevent the others from being processed.
 */
public class SimpleRetryableKinesisClient<T extends Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleRetryableKinesisClient.class);

    private final AmazonKinesisAsync amazonKinesisClient;

    private final String stream;

    private final StringTemplate partitionKeyTemplate;

    SimpleRetryableKinesisClient(AmazonKinesisAsync amazonKinesisClient, String stream, String partitionKey) {
        this.amazonKinesisClient = amazonKinesisClient;
        this.stream = stream;
        this.partitionKeyTemplate = StringTemplate.compile(partitionKey);
    }

    /**
     * Puts a single record to kinesis. It is recommended to always buffer into multiple
     * events and do putRecords instead.
     */
    public Observable<T> putRecord(T event) {
        amazonKinesisClient.putRecord(stream, event.raw().asByteBuffer(),
                partitionKeyTemplate.format(event).get());
        return Observable.just(event);
    }

    /**
     * Asynchronously puts records to kinesis.
     *
     * @param events - List of events to send
     * @return - Observable with same list as parameter
     */
    public Observable<List<T>> putRecords(List<T> events) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("putRecords() with {} events", events.size());
        }

        RequestContext request = new RequestContext(events, new PutRecordsRequest()
                .withRecords(events.stream()
                        .map(this::toRecordEntries)
                        .collect(toList()))
                .withStreamName(stream));
        putRecordsAsync(request);

        return request.subject;

    }


    /**
     * Recursively retry until there are no more failed records in response or to many retries
     */
    private void putRecordsAsync(final RequestContext request) {
        amazonKinesisClient.putRecordsAsync(request.putRecordsRequest, new AsyncHandler<PutRecordsRequest, PutRecordsResult>() {

            @Override
            public void onError(Exception exception) {
                request.error(exception);
            }

            @Override
            public void onSuccess(PutRecordsRequest putRecordsRequest, PutRecordsResult putRecordsResult) {
                // Surround with try/catch to prevent any unexpected exceptions from beeing swallowed
                try {
                    if (putRecordsResult.getFailedRecordCount() > 0) {
                        LOGGER.debug("Got {} failed records, retrying (attempts = {})",
                                putRecordsResult.getFailedRecordCount(), request.attempt);
                        // Try again with failing records,
                        Optional<RequestContext> nextAttempt = request.nextAttempt(putRecordsResult);
                        if (nextAttempt.isPresent()) {
                            putRecordsAsync(nextAttempt.get());
                        } else {
                            request.error(new FatalAWSException("Too many kinesis retries"));
                        }
                    } else {
                        request.done();
                    }
                } catch (Throwable t) {
                    LOGGER.error("Unexpected exception in onSuccess()", t);
                    request.error(t);
                }
            }
        });
    }

    /**
     * Converts event to actual kinesis entry type
     */
    private PutRecordsRequestEntry toRecordEntries(T event) {
        Optional<String> partitionKey = partitionKeyTemplate.format(event);
        return new PutRecordsRequestEntry().withData (
                event.raw().asByteBuffer())
                // FIXME: If partitionkey does not return a value, what approach is best?
                .withPartitionKey(partitionKey.isPresent()
                        ? partitionKey.get()
                        : UUID.randomUUID().toString());
    }


    /**
     * Contains state in order to track retries as well as returning response to pipeline.
     */
    private static class RequestContext<E extends Event> {

        private static final int MAX = 20; // Temporary until proper retry strategy

        public final ReplaySubject<List<E>> subject = ReplaySubject.createWithSize(1);

        /**
         * Keep them here until we are done, then return them
         */
        public final List<E> events;

        /**
         * Request to execute
         */
        public PutRecordsRequest putRecordsRequest;

        /**
         * Attempt count
         */
        public AtomicInteger attempt = new AtomicInteger(1);

        public RequestContext(List<E> events, PutRecordsRequest putRecordsRequest) {
            this.events = events;
            this.putRecordsRequest = putRecordsRequest;
        }

        private boolean hasNextAttempt() {
            return attempt.get() > MAX ? false : true;
        }


        /**
         * Based on non successful records, returns a RequestContext with a correct PutRecordsRequest
         *
         * @param result is the last result returned from Kinesis
         * @return RequestContext IF there are more attempts, Optional.empty() otherwise
         */
        public Optional<RequestContext> nextAttempt(PutRecordsResult result) {
            this.attempt.incrementAndGet();
            if (!hasNextAttempt()) {
                return Optional.empty();
            }
            this.putRecordsRequest = failedRecords(result);

            // TODO - Implement retry strategy properly
            try {
                Thread.sleep(500); // To high? Configurable?
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return Optional.of(this);
        }

        /**
         * Based on the request and the result, returns a new request
         * containing the records that failed.
         * @param result is the last PutRecordsResult
         * @return a new PutRecordsRequest with failing records
         */
        private PutRecordsRequest failedRecords(PutRecordsResult result) {
            List<PutRecordsRequestEntry> newRecords = new ArrayList<>();
            List<PutRecordsResultEntry> records = result.getRecords();
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).getErrorCode() != null) {
                    newRecords.add(putRecordsRequest.getRecords().get(i));
                }
            }
            return new PutRecordsRequest()
                    .withRecords(newRecords)
                    .withStreamName(putRecordsRequest.getStreamName());
        }

        /**
         * Invoked after request is successful and there are no failing records.
         */
        public void done() {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Done() mem free {}, mem max {}", Runtime.getRuntime().freeMemory(), Runtime.getRuntime().maxMemory());
            }
            this.subject.onNext(events);
            this.subject.onCompleted();
        }

        /**
         * Invoked if an error occurs of there are no more retries
         * @param t is the original exception
         */
        public void error(Throwable t) {
            this.subject.onError(t);
        }
    }
}
