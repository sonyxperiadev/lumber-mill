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
import lumbermill.api.Event;
import lumbermill.api.EventProcessor;

/**
 * Implement this interface if you need access to the Lambda Context object
 */
public interface LambdaContextAwareEventProcessor<T extends Event> extends EventProcessor {

    void initialize(Context context);
}
