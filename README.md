[![Build Status](https://travis-ci.org/sonyxperiadev/lumber-mill.svg?branch=master)](https://travis-ci.org/sonyxperiadev/lumber-mill) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sonymobile/lumbermill-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sonymobile/lumbermill-core)
# Lumber Mill

*Lumber Mill is under heavy development/refactoring so expect api changes to occur*

### What is Lumber Mill?
*Log Collection & Processing on AWS, with focus on fetching logs from S3, Cloudwatch Logs and Kinesis.
It currently supports for writing to Kinesis, S3, Elasticsearch (incl AWS Elasticsearch Service), Influxdb & Graphite. It also 
has support for mutating functions like grok, compression etc. The processing pipeline is based on RxJava.*


Sample AWS Lambda sample that receives events from Cloudwatch Logs and stores in AWS Elasticsearch Service.

```groovy
public class DemoEventProcessor extends CloudWatchLogsLambda implements EventProcessor {

    public DemoEventProcessor() {
        super(this)
    }

    Observable call(Observable observable) {
        observable

        // Parse and de-normalize events
        .compose ( new CloudWatchLogsEventPreProcessor())
        .flatMap (  
            grok.parse (
               field:        'message',
               pattern:      '%{AWS_LAMBDA_REQUEST_REPORT}'))
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
```


### Documentation
* [http://lumber-mill.readthedocs.io/en/latest/](http://lumber-mill.readthedocs.io/en/latest/)
* Simple samples are in the lumbermill-simple-samples module

### Changes
[View Changelog](CHANGELOG.md)

### Build

    ./gradlew clean build
    
### Binaries

Maven

    <dependency>
          <groupId>com.sonymobile</groupId>
          <artifactId>lumbermill-aws-lambda</artifactId>
          <version>0.0.21</version>
    </dependency>

Gradle

    compile 'com.sonymobile:lumbermill-aws-lambda:0.0.21'
    
### Docker

Try out some samples, [Docker on the wiki](https://github.com/sonyxperiadev/lumber-mill/wiki/0.1.-Run-with-docker)

This will run and list available samples you can run

    docker run lifelog/lumber-mill 
    
#### Sample: Index a local file with docker

You can try to index a file locally (you must have elasticsearch running) with docker. The sample below will index README.md
in the directory where you are currently executing. **Note**, you do not need to check out lumbermill to try this but the file
must exist.

    export ES_URL=http://_your_es_host_:9200
     
    docker run --rm \
    -e buffer=10 \
    -v $(pwd):/home \
    -e file=/home/README.md \
    -e es_url=$ES_URL \
    lifelog/lumber-mill elasticsearch-fs.groovy
    
If you need basic auth add
    
    -e user=username -e passwd=somepasswd


The code for indexing a single file looks like this, complete code is available under lumbermill-simple-samples.    

```groovy
file.readFileAsLines (
        file:  '{file}',
        codec : Codecs.TEXT_TO_JSON)

.buffer(env('buffer','10').number())

.flatMap (
    elasticsearch.client(
            basic_auth:   '{user   || }:{passwd || }',
            url:          '{es_url || http://localhost:9200}',
            index_prefix: '{index  || lumbermill}-',
            type:         '{fs     || fs}',
            dispatcher: [
                    max_concurrent_requests: '{max_req || 5}'
            ]
        ))
.doOnError({t -> t.printStackTrace()})
.subscribe()
```
    
### \*.internal.\*

All code inside the \*.internal.\* packages is considered private API and should not be relied upon at all. It can change at any time.
    
