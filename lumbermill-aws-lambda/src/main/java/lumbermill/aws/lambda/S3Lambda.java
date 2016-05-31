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
package lumbermill.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;

import lumbermill.api.EventProcessor;
import lumbermill.api.JsonEvent;
import rx.Observable;


/**
 * Invoked by the AWS Lambda runtime
 */
@SuppressWarnings("unused")
public abstract class S3Lambda implements RequestHandler<S3Event, String> {

    private EventProcessor eventProcessor;

    public S3Lambda(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public String handleRequest(S3Event s3, Context context) {
        try {
            Observable.from(s3.getRecords())
                    .map(this::toJson)
                    .compose(eventProcessor)
                    .toBlocking().subscribe();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
        return "done";
    }

    protected JsonEvent toJson(S3EventNotification.S3EventNotificationRecord record) {
       return new JsonEvent()
            .put("bucket_name", record.getS3().getBucket().getName())
                .put("bucket_arn", record.getS3().getBucket().getArn())
                .put("key", record.getS3().getObject().getKey())
                .put("etag", record.getS3().getObject().geteTag())
                .put("size", record.getS3().getObject().getSizeAsLong());
    }

}
