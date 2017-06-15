[![Build Status](https://travis-ci.org/sonyxperiadev/lumber-mill.svg?branch=master)](https://travis-ci.org/sonyxperiadev/lumber-mill) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.sonymobile/lumbermill-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.sonymobile/lumbermill-core)
# Lumber Mill

*Lumber Mill is under heavy development/refactoring so expect api changes to occur*

### Documentation
* Documentation & Samples at [http://lumber-mill.readthedocs.io/en/latest/](http://lumber-mill.readthedocs.io/en/latest/)
* Executable & Deployable samples at [https://github.com/sonyxperiadev/lumber-mill-samples](https://github.com/sonyxperiadev/lumber-mill-samples)
* Checkout some simple runnable samples in the lumbermill-simple-samples module.

### What is Lumber Mill?
*Reactive API for Log Collection & Processing on AWS, with focus on fetching logs from S3, Cloudwatch Logs and Kinesis.
It currently supports writing to Kinesis, S3, Elasticsearch (incl AWS Elasticsearch Service), Influxdb & Graphite. It also 
has support for enriching functions like grok, compression etc. The processing pipeline is based on RxJava.*

### Changes
[View Changelog](CHANGELOG.md)

### Build

    ./gradlew clean build
    
### Binaries

Maven

    <dependency>
          <groupId>com.sonymobile</groupId>
          <artifactId>lumbermill-{module}</artifactId>
          <version>0.0.25</version>
    </dependency>

Gradle

    compile 'com.sonymobile:lumbermill-{module}:0.0.25'
    
### \*.internal.\*

All code inside the \*.internal.\* packages is considered private API and should not be relied upon at all. It can change at any time.
    
