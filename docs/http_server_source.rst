HTTP/REST Server
================

Lumbermill Http/Rest support wraps (the superb library) vertx to provide a simple but yet powerful way of ingesting
data with http.

*We are running this module and ingesting large amount of data but it is currently limited in how much you can configure
your rest endpoints. GET is not implemented other than a 200 OK healthcheck*

**POST with path param and query params**

This samples shows how to setup an endpoint to receive Observable for each request containing path parameters, query parameters
and body. Path parameters and query parameters are stored as metadata and not in the body.

.. code-block:: groovy

    import lumbermill.api.Event
    import static lumbermill.Http.http
    import static lumbermill.Core.*

    http.server(port:8080)
        .post (
            path : '/person/:name',
            tags : ['person'])
        .onTag('person', { request ->
            request
                .doOnNext(console.stdout('Name is {name}'))
                .doOnNext(console.stdout('Lastname is {lastname}'))
                .doOnNext{Event event -> println event.raw().utf8()}
        })

Running the following curl command should produce the output below

.. code-block:: shell

    curl -v  -XPOST localhost:8080/person/johan?lastname=rask -d 'Hello world' in the console.

    Name is johan
    Lastname is rask
    {"message":"Hello world","@timestamp":"2016-12-06T10:57:04.324+01:00","tags":["person"]}


If you want to handle all data in a single method and skip tags you can use *on()* instead of *onTag()*.

.. code-block:: groovy

    http.server(port:8080)
        .post (
            path : '/person/:name')
        .on({ request ->
            request
                .doOnNext(console.stdout('Name is {name}'))
                .doOnNext{Event event -> println event.raw().utf8()}
        })


**Content-Type and codecs**

These are the default Codecs used when receiving data.

* application/json : Codecs.JSON_ANY
* text/plain : Codecs.TEXT_TO_JSON
* default : Codecs.TEXT_TO_JSON

To change how data is decoded you can set the codec when creating an endpoint.
The example below will simply print 'Hello world' (if using the curl command above)

.. code-block:: groovy

    http.server(port:8080)
        .post (
            path : '/person/:name'),
            codec: Codecs.BYTES     // Will simply wrap the body as raw bytes
        .on({ request ->
            request
                .doOnNext{Event event -> println event.raw().utf8()}
        })