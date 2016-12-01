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

import lumbermill.api.EventProcessor;
import lumbermill.api.JsonEvent;
import rx.Observable;

import static lumbermill.Core.grok;
import static lumbermill.internal.MapWrap.Map;

/**
 * Decodes raw VPC Flow Log messages from Cloudwatch logs
 */
@SuppressWarnings("unused")
public class VPCFlowLogsEventPreProcessor implements EventProcessor<JsonEvent, JsonEvent>{

    public Observable<JsonEvent> call(Observable<JsonEvent> observable) {

        return observable
                .compose(new CloudWatchLogsEventPreProcessor())
                .flatMap(
                        grok.parse (
                                Map("field",  "message",
                                    "pattern","%{AWS_FLOW_LOG}")));
    }
}
