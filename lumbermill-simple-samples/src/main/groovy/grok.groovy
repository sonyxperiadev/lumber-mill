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
import lumbermill.Core
import lumbermill.api.Codecs
import rx.Observable

import static lumbermill.Core.*

/*
 Simple demonstration of grok usage.
 */
Observable.just(Codecs.TEXT_TO_JSON.from("1 times"))
    .flatMap (
        Core.grok.parse (
            field  : 'message',
            pattern: '%{NUMBER:number:int} %{WORD:text}'
        )
    )
    .doOnNext(console.stdout())
    .toBlocking()
    .subscribe()

