AWS Lambda Source
=================

Lumber-Mill supports getting events with AWS Lambda from three different sources.

* Kinesis
* S3
* Cloudwatch logs (incl. cloudtrail, AWS Flowlogs)

Kinesis
-------

To receive events from Kinesis, you must extend *lumbermill.aws.lambda.KinesisLambda*. The event contents will be the raw
contents of the kinesis record.

.. code-block:: groovy

    import lumbermill.api.EventProcessor
    import lumbermill.aws.lambda.KinesisLambda
    import rx.Observable
    import static lumbermill.Core.*

    public class DemoLambda extends KinesisLambda {

    public DemoLambda() {
        super(new DemoLambdaEventProcessor());
    }

    public static class DemoLambdaEventProcessor implements EventProcessor {

        // Raw contents of kinesis event
        Observable call(Observable observable) {
            observable

            // Convert to json if expecting json
            .flatMap ( toJsonObject() )

            // Time behind latest is stored as metadata
            .doOnNext (console.stdout('Currently behind latest with {millisBehindLatest} ms')
            .doOnNext( console.stdout())

S3
--

To receive events from S3, you must extend *lumbermill.aws.lambda.S3Lambda*. The event is
a lumbermill.api.JsonEvent with contents the metadata of the event as fields, the
following fields exists.

* bucket_name
* key
* bucket_arn
* etag
* size

.. code-block:: groovy

    import lumbermill.api.EventProcessor
    import lumbermill.aws.lambda.S3Lambda
    import rx.Observable
    import static lumbermill.Core.*
    import static lumbermill.AWS.s3

    public class DemoLambda extends S3Lambda {

    public DemoLambda() {
        super(new DemoLambdaEventProcessor());
    }

    public static class DemoLambdaEventProcessor implements EventProcessor {

        Observable call(Observable observable) {
            observable

            // Since the event is only a reference, the file must be downloaded
            .flatMap (
                s3.download (
                    bucket: '{bucket_name}',
                    key: '{key}',
                    remove: true
                )
            )
            // Then (if you want), read each line as a separate Event
            .flatMap (
                file.lines(file: '{s3_download_path}')
            )
            .doOnNext( console.stdout())

Cloudwatch Logs
---------------

Receiving Cloudwatch Logs events is similar to both S3 and Kinesis. First subclass
*lumbermill.aws.lambda.CloudWatchLogsLambda*.

The data the is received in the call() method is encoded data which must be decoded, and this
is done with the *lumbermill.aws.lambda.CloudWatchLogsEventPreProcessor* which will decode, parse
and denormalize the data into a stream of JsonEvents.

Each JsonEvent contains the fields

* message
* logGroup
* logStream
* @timestamp

.. code-block:: groovy

    import lumbermill.api.Codecs
    import lumbermill.api.JsonEvent
    import lumbermill.aws.lambda.CloudWatchLogsLambda
    import lumbermill.aws.lambda.CloudWatchLogsEventPreProcessor
    import rx.Observable

    import static lumbermill.Core.*

    public class DemoLambda extends CloudWatchLogsLambda {
        public DemoLambda() {
            super(new DemoLambdaEventProcessor());
        }

        private static class DemoLambdaEventProcessor implements LambdaContextAwareEventProcessor {

            Observable call(Observable observable) {

                // Parse and de-normalize events (required as first transformer)
                // Will return JsonEvent
                .compose (
                    new CloudWatchLogsEventPreProcessor()
                )
                .doOnNext(console.stdout())
            }

VPC Flow Logs
_____________

VPC Flow Logs events are received from Cloudwatch logs and the raw json is stored in the 'message' field. What we need
to do is to extract this and convert it to JsonEvent and this is done with the *lumbermill.aws.lambda.VPCFlowLogsEventPreProcessor*

.. code-block:: groovy

    Observable call(Observable observable) {
        .compose (
            new VPCFlowLogsEventPreProcessor()
        )
        .doOnNext(console.stdout())
    }

The JsonEvent has the following fields:

.. code-block:: json

     {
        "account_id" : "808736257386",
        "action" : "ACCEPT",
        "bytes" : 1990,
        "dstaddr" : "52.30.151.45",
        "dstport" : "443",
        "end" : 1480508691,
        "interface_id" : "eni-3a2b2575",
        "log_status" : "OK",
        "packets" : 11,
        "protocol" : "6",
        "srcaddr" : "172.31.21.142",
        "srcport" : "35052",
        "start" : 1480508631,
        "version" : "2"
    }

Cloudtrail
__________

Cloudtrail events are received from Cloudwatch logs and the raw json is stored in the 'message' field. What we need
to do is to extract this and convert it to JsonEvent. This will be a separate EventProcessor in next release of Lumber-Mill in
the same way as with AWS Vpc Flow Logs.

.. code-block:: groovy

    Observable call(Observable observable) {
        .compose (
            new CloudWatchLogsEventPreProcessor()
        )

        // Decodes 'message' field and merge new and old event
        .flatMap ({ JsonEvent -> event
            return Codecs.JSON_OBJECT.from(e.valueAsString('message'))
                            .merge(e)
                            .toObservable()})
        .doOnNext(console.stdout())
    }
