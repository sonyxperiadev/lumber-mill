import lumbermill.api.Codecs
import lumbermill.api.Event

import static lumbermill.Http.http
import static lumbermill.Core.*

/*
  Usage with curl:

    curl -v  -XPOST localhost:8080/person/johan?lastname=rask -d 'Hello world'
 */
http.server(port:8080)
    .post (
        path : '/person/:name',
        codec: Codecs.BYTES)
    .on({ request ->
        request
                .doOnNext(console.stdout('Name is {name} {lastname}'))
                .doOnNext{Event event -> println event.raw().utf8()}
    })