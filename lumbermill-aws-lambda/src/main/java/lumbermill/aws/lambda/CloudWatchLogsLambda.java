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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.EventProcessor;
import rx.Observable;

import java.util.Map;

@SuppressWarnings("unused")
public abstract class CloudWatchLogsLambda implements RequestHandler<Map<String, Map<String,String>>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private EventProcessor eventProcessor;

    public CloudWatchLogsLambda(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    @Override
    public String handleRequest(Map<String, Map<String, String>> event, Context context) {
        Observable.just(event)
                .map(this::toEvent)
                .compose(eventProcessor)
                .toBlocking().subscribe();
        return "Done";
    }

    /**
     * Decodes the cloudwatch event as a JsonEvent
     */
    protected Event toEvent(Map<String, Map<String, String>> event)  {
        try {
            return Codecs.JSON_OBJECT.from(objectMapper.writeValueAsBytes(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
