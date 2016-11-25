Elasticsearch
=============

Stores events in Elasticsearch

**Usage**

*build.gradle*

.. code-block:: groovy

    compile 'com.sonymobile:lumbermil-elasticsearch-client:$version'


*Groovy script*

.. code-block:: groovy

    import lumbermill.api.Codecs
    import static lumbermill.Core.*
    import static lumbermill.Elasticsearch.elasticsearch

    Observable.just(Codecs.TEXT_TO_JSON.from("hello"), Codecs.TEXT_TO_JSON.from("World"))
        .flatMap (
            fingerprint.md5('{message}')
        )
        .buffer (100) // Buffering is currently required. Pick a suitable amount.
        .flatMap (
            elasticsearch.client (
                basic_auth: 'user:passwd',         // Optional
                url: 'http(s)://host',             // Required
                index_prefix: 'myindex-',          // Required, supports pattern '{anIndex}-'
                type: 'a_type',                    // Required, supports pattern '{type}'
                document_id: '{fingerprint}',      // Optional, but recommended
                timestamp_field: '@timestamp'      // Optional, defaults to @timestamp
                retry: [                           // Optional, defaults to fixed, 2000, 20
                    policy: 'linear',
                    attempts: 20,
                    delayMs: 500
                ],
                dispatcher: [                      // Optional
                    max_concurrent_requests: 2,    // Optional, defaults to 5
                    threadpool: <ExecutorService>, // Optional
                ]
            )
        )
        .toBlocking()
        .subscribe()


**Arguments**

Elasticsearch requires a List<JsonEvent> as input so you *MUST* buffer before sending. It will convert events into
a single Bulk API request.

**Returns**

Elasticsearch returns Observable<ElasticSearchResponseEvent> which extends JsonEvent and contains the actual
raw response from Elasticsearch. If you want to continue working with the original Events that where sent
as arguments to Elasticsearch function you can get those with the arguments() method.

.. code-block:: groovy

    o.flatMap (
        elasticsearch.client (...).flatMap(response.arguments())
    )


**Errors**

Elasticsearch client is built to handle partial errors, meaning that some entries are not properly stored.
This could be due to anything from malformed content or shard failures. The Elasticsearch client will
retry any failures that are not unrecoverable *(400 BAD_REQUEST)*, those will simply be ignored and not retried.

**Retries**

Elasticsearch has a default retry policy that is fixed delay of 2 seconds and 20 attempts,
so it will retry failed records every 2 seconds 20 times.

Once there are no more retries, an FatalIndexException is thrown to indicate that it failed and there is no use to continue.


**Limitations**

* Currently it only uses *index* operation, does not support create, update or delete.
* Only daily indices can be created.

**Performance**

It is a custom implementation based on OkHttp. We started out with Jest but could not get good enough throughput,
but OkHttp has proven to be amazing.