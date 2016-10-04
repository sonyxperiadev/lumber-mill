/**
 * Created by 23061174 on 9/28/16.
 */


import lumbermill.Core
import lumbermill.api.Codecs
import lumbermill.api.Observables
import lumbermill.internal.net.VertxTCPServer
import rx.Observable
import static lumbermill.Graphite.graphite

VertxTCPServer server = new VertxTCPServer("localhost", 2003).listen({event -> println 'Rec:' + event.raw().utf8()})

Codecs.TEXT_TO_JSON.from("hello")
    .put("@metric", "hits.count")
    .put("@value", 5)
    .put("time", System.currentTimeMillis())
    .toObservable()
    .flatMap (
        graphite (
            //host: '',
            //port: 2003,
            timestamp_field: 'time',
            timestamp_precision: 'MILLIS',
            metrics : [
                    'stats.counters.{@metric}' : '{@value}',
                    'stats.counters.2.{@metric}' : '{@value}'
            ]
        )
).toBlocking().subscribe()

Thread.sleep(1000)
