Kinesis Consumer Library
========================

Lumber-Mill can use KCL to process data. Each 'batch' is received as a stream and checkpointed after
it successfully returns. Currently there is no support for delay checkpointing.

**Build**

.. code-block:: groovy

    compile 'com.sonymobile:lumbermill-aws-kcl:$version'


This sample subscribes to a kinesis stream and simply prints the contents of each record and the total count.

.. code-block:: groovy

    import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
    import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
    import lumbermill.api.BytesEvent
    import lumbermill.aws.kcl.KCL
    import static lumbermill.api.Sys.env
    import static lumbermill.aws.kcl.KCL.workerId


    // Uses minimal KCL configuration
    KCL.create (
        new KinesisClientLibConfiguration (
            env ('appName', 'testApp').string(),
            env ('streamName', 'testStream').string(),
            new DefaultAWSCredentialsProviderChain(),
            workerId())
        .withRegionName(env ("region", "eu-west-1").string()))

        .dry(env ("dry", "false").bool()) // Dry will not checkpoint

        // Each record as an observable
        .handleRecordBatch { record ->
            record
                .doOnNext{BytesEvent event -> println event.raw().utf8()}
                .count()
                .doOnNext{count -> println count} // Prints the total number of records that was received.
        }

