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
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import lumbermill.api.BytesEvent;
import lumbermill.api.Codecs;
import lumbermill.api.EventProcessor;
import rx.Observable;


/**
 * Invoked by the AWS Lambda runtime
 */
@SuppressWarnings("unused")
public abstract class KinesisLambda implements RequestHandler<KinesisEvent, String> {

    public static final String METADATA_KINESIS_EVENT_RECORD = "kinesisEventRecord";
    public static final String METADATA_MILLIS_BEHIND_LATEST = "millisBehindLatest";

    private EventProcessor eventProcessor;

    public KinesisLambda(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public String handleRequest(KinesisEvent event, Context context) {
        if (eventProcessor instanceof LambdaContextAwareEventProcessor) {
            ((LambdaContextAwareEventProcessor)eventProcessor).initialize(context);
        }

        Observable.from(event.getRecords())
                .map(this::toBytes)
                .compose(eventProcessor)
                .count()
                .toBlocking()
                .subscribe();
        return "Done";
    }

    protected BytesEvent toBytes(KinesisEvent.KinesisEventRecord record) {
        BytesEvent bytesEvent = Codecs.BYTES.from(record.getKinesis().getData().array());
        bytesEvent.put(METADATA_KINESIS_EVENT_RECORD, record);
        bytesEvent.put(METADATA_MILLIS_BEHIND_LATEST, System.currentTimeMillis() - record.getKinesis().getApproximateArrivalTimestamp().getTime());
        return bytesEvent;
    }

}
