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
package lumbermill;

import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import lumbermill.internal.StringTemplate;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action;
import rx.functions.Action1;
import rx.functions.Func1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class Console<T extends Event> {

    public Console() {}

    public Action1<T> stdout() {
        return t -> System.out.println(t);
    }

    public Action1<T> stdout(String pattern) {
        StringTemplate st = StringTemplate.compile(pattern);
        return t -> System.out.println(st.format(t).get());
    }

    public Action1<List<T>> bstdout() {
        return t -> System.out.println(t);
    }


    public Observable<JsonEvent> stdin() {

        return Observable.create (
                new Observable.OnSubscribe<JsonEvent>() {
                    BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));

                    @Override
                    public void call(Subscriber<? super JsonEvent> sub) {
                        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                            sub.onCompleted();
                        }));

                        while (true) {
                            try {
                                sub.onNext(Codecs.TEXT_TO_JSON.from(lineReader.readLine()));
                            } catch (IOException e) {
                                sub.onError(e);
                            }
                        }
                    }
                }
        );
    }
}
