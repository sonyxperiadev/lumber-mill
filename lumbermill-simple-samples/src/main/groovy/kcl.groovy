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

    // Dry will not checkpoint
    .dry(env ("dry", "false").bool())

    // Each record as an observable
    .handleRecordBatch { record ->
        record

            // Print raw string
            .doOnNext{BytesEvent event -> println event.raw().utf8()}

            // Count and print count
            .count()
            .doOnNext{count -> println count}
    }