AWS Elasticsearch Service
=========================


Stores events in AWS Elasticsearch Service. The configuration is the same as "normal" Elasticsearch but you need to import
the aws module instead, and also configure which region that is used.

**Usage**

*build.gradle*

.. code-block:: groovy

    compile 'com.sonymobile:lumbermill-aws:$version'


*Groovy script*

.. code-block:: groovy

    import lumbermill.api.Codecs
    import lumbermill.AWS
    import static lumbermill.Core.*

    Observable.just(Codecs.TEXT_TO_JSON.from("hello"), Codecs.TEXT_TO_JSON.from("World"))
        .flatMap (
            fingerprint.md5('{message}')
        )
        .buffer (100) // Buffering is currently required. Pick a suitable amount.
        .flatMap (
            AWS.elasticsearch.client (
                // Same options as for "normal" Elasticsearch

                region : 'eu-west-1'  // Optional, defaults to eu-west-1
            )
        )
        .toBlocking()
        .subscribe()
