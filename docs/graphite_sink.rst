Graphite
========

Stores metrics in graphite using carbon line protocol.

**Usage**

*build.gradle*

.. code-block:: groovy

    compile 'com.sonymobile:lumbermill-graphite:$version'


*Groovy script*

This simple sample will write two metrics from a single event to graphite.

.. code-block:: groovy

    import lumbermill.api.Codecs
    import static lumbermill.Graphite.carbon

    Codecs.TEXT_TO_JSON.from("hello")
        .put("@metric", "hits.count")
        .put("@value", 5)
        .toObservable()
        .flatMap (
            carbon (
                host: 'localhost',                            // Optional (localhost)
                port: 2003,                                   // Optional (2003)
                timestamp_field: '@timestamp',                // Optional (@timestamp)
                timestamp_precision: 'ISO_8601',              // Optional (ISO_8601). Supports 'MILLIS' and 'SECONDS'
                metrics : [                                   // At least one metric is required
                  'stats.counters.{@metric}' : '{@value}',          // If either @metric or @value is missing no metric is stored
                  'stats.counters.duplicate.{@metric}' : '{@value}'
                ]
            )
    ).toBlocking()
    .subscribe()


**Limitations**

Since there is no way of supporting updates, at-least-once delivery is not supported. Any event that comes more than
once will be stored again.