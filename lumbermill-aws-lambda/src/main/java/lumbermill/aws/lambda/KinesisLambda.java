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
import lumbermill.Core;
import lumbermill.api.BytesEvent;
import lumbermill.api.Codecs;
import lumbermill.api.EventProcessor;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;


/**
 * Invoked by the AWS Lambda runtime
 */
@SuppressWarnings("unused")
public abstract class KinesisLambda implements RequestHandler<KinesisEvent, String> {

    private EventProcessor eventProcessor;

    public KinesisLambda(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public String handleRequest(KinesisEvent event, Context context) {

        Observable.from(event.getRecords())
                .map(this::toBytes)
                .compose(eventProcessor)
                .count()
                .doOnNext(cnt -> System.out.println("Total count: " + cnt))
                .toBlocking()
                .subscribe();
        return "Done";
    }

    protected BytesEvent toBytes(KinesisEvent.KinesisEventRecord record) {
        return Codecs.BYTES.from(record.getKinesis().getData().array());
    }

}
