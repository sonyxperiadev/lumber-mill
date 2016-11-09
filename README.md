[![Build Status](https://travis-ci.org/sonyxperiadev/lumber-mill.svg?branch=master)](https://travis-ci.org/sonyxperiadev/lumber-mill) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sonymobile/lumbermill-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sonymobile/lumbermill-core)
# Lumber Mill

*Lumber Mill is under heavy development/refactoring so expect api changes to occur*

### What is Lumber Mill?
A lot of things, but the focus is on Log Collection & Processing on AWS, fokus on S3, Cloudwatch Logs and Kinesis.
Support for writing to Kinesis, S3, Elasticsearch (inkl AWS Elasticsearch Service), Influxdb, Graphite. It also 
has support for mutating functions like grok etc. The processing pipeline is based on RxJava.


AWS Lambda sample that receives events from Cloudwatch Logs and stores in AWS Elasticsearch Service.

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
* Simple samples are in the lumbermill-simple-samples directory

### Changes
[View Changelog](CHANGELOG.md)

### Build

    ./gradlew clean build
    
### Binaries

Maven

    <dependency>
          <groupId>com.sonymobile</groupId>
          <artifactId>lumbermill-aws-lambda</artifactId>
          <version>0.0.19</version>
    </dependency>

Gradle

    compile 'com.sonymobile:lumbermill-aws-lambda:0.0.19'
    
### Docker

Try out some samples, [Docker on the wiki](https://github.com/sonyxperiadev/lumber-mill/wiki/0.1.-Run-with-docker)

This will run and list available samples you can run

    docker run lifelog/lumber-mill 
    
### \*.internal.\*

All code inside the \*.internal.\* packages is considered private API and should not be relied upon at all. It can change at any time.
    
