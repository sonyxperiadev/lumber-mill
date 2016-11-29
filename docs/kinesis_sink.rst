Kinesis
=======

Stores metrics in Kinesis using aws-sdk. It has built-in retry functionality.

**Usage**

*build.gradle*

.. code-block:: groovy

    compile 'com.sonymobile:lumbermill-aws:$version'


*Groovy script*

Sample will invoke putRecords() with two events. Even if the buffer says 100, onCompleted will be invoked
after the two events have been processed which will cause the pipeline to flush.

.. code-block:: groovy

    import lumbermill.api.Codecs
    import lumbermill.Core.*
    import lumbermill.AWS.*

    Observable.just(Codecs.TEXT_TO_JSON.from("hello"), Codecs.TEXT_TO_JSON.from("World"))
        .buffer (100)
        .flatMap (
            kinesis.bufferedProducer (
                region: 'eu-west-1',          // Optional, defaults to eu-west-1, overridded by endpoint
                endpoint: 'host',             // Optional, for custom hostname
                stream: 'stream_name',        // Required
                partition_key: '{afield}',    // Optional (**Recommended**), supports patterns. defaults to randomized uuid
                max_connections: 10,          // Optional, defaults to 10.
                request_timeout: 60000,       // Optional, defaults to 60000ms
                retry: [                      // Optional, defaults to fixed, 2000, 20
                    policy: 'linear',
                    attempts: 20,
                    delayMs: 500
                ]
            )
        )
        .toBlocking()
        .subscribe()