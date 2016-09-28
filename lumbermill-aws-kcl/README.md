# KCL (Kinesis Consumer Library)

Alternative when running as AWS Lambda is not suitable.

Dummy sample of using KCL to 'shovel' data from one stream to another.

```groovy
KCL.create (
    new KinesisClientLibConfiguration (
        'appName',
        'streamName',
        new DefaultAWSCredentialsProviderChain(),
        KCL.workerId())
    .withRegionName("eu-west-1")

    // Dry will not checkpoint
    .dry(false)

    // Each record as an observable
    .handleRecordBatch { record ->
        record
            .buffer(300)
            .flatMap (
               kinesis.bufferedProducer (
                 region: 'eu-west-1',        
                 stream: '<another_stream>'   
            ))
    }

```