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

