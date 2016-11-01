One minute intro
===============

Short introduction of the background of Lumber-Mill, why it exists and who it is usable for.

Why, when and for who?
______________________

**Why did we build it?**

To make a long story short, it started when we wanted to collect our AWS ELB logs on multiple accounts and send to Elasticsearch. This seemed
like a quite simple task for logstash but we had a pain with it. Except for this, doing custom "stuff" is hard in an off-the-shelf solution.
After some consideration we decided to implement our own solution for how to collect and process logs (and more) on with focus on AWS.

**What is it?**

So, what is Lumber-Mill? Lumber-Mill is a Reactive (RxJava) API that can be used from any JVM compatible language. Currently Groovy is the best supported
language but we are considering the best options to make it usable for all jvm languages. We started out implementing our own pipeline similar to
Logstash with different queues but decided to drop that approach and go all in for Rx instead which we have been really happy about.

Why Java/jvm? It feels like the jvm has the best third-party libraries, support for AWS lambdas and a good fit for our use case where we
often have high concurrency, io and cpu.

*Some of the features include*

 * AWS Lambda support (Kinesis, S3, Cloudwatch Logs, Cloudtrail)
 * S3 Get, Put, Delete
 * Kinesis Producer / Consumer
 * Grok, Compression, Templating, Base64, Json, Conditionals, Timestamps etc.
 * Elasticsearch Bulk API (including AWS Elasticsearch Service)
 * Graphite
 * InfluxDB

**When should you use it?**

If you run on AWS, you want a simple (not easy) way of collecting your logs. Perhaps you want to run it as AWS Lambdas and connect
to S3, Kines and/or Cloudwatch Logs.

**Who is it for?**

Lumber-Mill is not designed for the non-programmer, even if he or she is likely to get it to work. Lumber-Mill is designed
for programmers/devops/SRE etc with a professional programming background but with an emergent interest for devops, monitoring and of course
log processing!

How? Samples please!
____________________

**Cloudwatch to Elasticsearch**

This is a complete sample of AWS Lambda that when triggered by Cloudwatch Logs events, it will decode and parse these events and
send to AWS Elaticsearch Service.

.. code-block:: groovy

    public class DemoEventProcessor extends CloudWatchLogsLambda implements EventProcessor {

        public DemoEventProcessor() {
            super(this)
        }

        Observable call(Observable observable) {
            observable

            // Parse and de-normalize events
            .compose ( new CloudWatchLogsEventPreProcessor())
            .flatMap ( addField('type','cloudwatchlogs'))
            .flatMap ( fingerprint.md5())
            .buffer (100)
            .flatMap (
                AWS.elasticsearch.client (
                    url:          'https://endpoint',
                    index_prefix: 'indexname-',
                    type:         '{type}',
                    region:       'eu-west-1',
                    document_id:  '{fingerprint}'
                )
            )
        }
    }

**S3**

This sample is an AWS Lambda that when triggered by an S3 event it will not only download and parse but will
also gzip and put the file back on S3 before processing the contents.

.. code-block:: groovy

    Observable call(Observable observable) {

        // Download locally, remove original on completed
        observable.flatMap (
            s3.download (
                bucket: '{bucket_name}',
                key: '{key}',
                remove: true
            )
        )

        // Compress file since we want compressed files on S3
        .flatMap (
            gzip.compress (
                file: '{s3_download_path}'
            )
        )

        // Put compressed file to S3 under processed directory
        .flatMap (
            s3.put (
                bucket: '{bucket_name}',
                key   : 'processed/{key}.gz',
                file  : '{gzip_path_compressed}'
            )
        )

        // Read each line
        .flatMap ( file.lines(file: '{s3_download_path}'))

        // Parse lines with grok => json, tag with _grokparsefailure on miss
        .flatMap (
            grok.parse (
                field:        'message',
                pattern:      '%{AWS_ELB_LOG}',
                tagOnFailure: true
            )
        )

        // Use correct timestamp
        .flatMap (
            rename (
                from: 'timestamp',
                to  : '@timestamp'
            )
        )
        .flatMap (
            addField ('type', 'elb')
        )
        .flatMap (
            fingerprint.md5('{message}')
        )
        // Buffer to suitable bulk size
        .buffer(5000)
        .flatMap (
           // See Elasticsearch in previous sample or use other output
        )
    }


Status?
_______

We use Lumber-Mill extensively to collect and process logs from different AWS accounts to our central system.
Before release, or even before we put it on master, we usually run it in production for quite some time.

We are currently thinking about the API and what the best approach is to make it as simple to work with and usable from multiple jvm languages.
Due to that, api:s might feel a bit awkward (well, it can suck) to work with when not using groovy.


Installation / Deployment
_________________________

**TODO**