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

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.handlers.AsyncHandler;
import rx.subjects.ReplaySubject;

/**
 * Bridges AWS requests to Observables.
 */
class Aws2Rx<REQ extends AmazonWebServiceRequest, RES> implements AsyncHandler<REQ,RES>{

    public final ReplaySubject<RES> subject;

    public Aws2Rx() {
        this.subject = ReplaySubject.createWithSize(1);
    }

    @Override
    public void onError(Exception exception) {
        subject.onError(exception);
    }

    @Override
    public void onSuccess(REQ request, RES result) {
        subject.onNext(result);
        subject.onCompleted();
    }
}
