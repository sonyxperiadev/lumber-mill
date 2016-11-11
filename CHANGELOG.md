# Lumber Mill Releases

### [Unreleased / master] Version 0.0.22-SNAPSHOT


### 0.0.21

 * Elasticsearch: Messages that causes 400 BAD_REQUEST are now correctly dropped
   and not retried.
   
 * Bumped kcl to 1.7.1 and use the same version for all aws-sdk

### 0.0.20

 * Basic auth for Elasticsearch
 * Influxdb is now async and non-blocking
 * Access to raw Kinesis event in AWS Lambda
 * Support for writing to graphite (carbon) using their line protocol
   Checkout README.md under graphite module.

### 0.0.19

 * Fixed bug in LinearTimer
 * Support for latest (1.0.x) influxdb
 * Retry on exception in kinesis publisher

### 0.0.18

* Fixed bug in kinesis client where retries default was 0 unless specified, is now 20.

### 0.0.17

* After months of running KCL internally we now release it with Lumbermill, docs will be updated but
  checkout lumbermill-simple-samples for a small runnable sample.
* Bumped RxJava and vertx to latest
* Bug fixes rename() was converting all types to String

### 0.0.16

* InfluxDB support
* Minor fixes

### 0.0.15

* Fixed serious bug in CloudwatchLogsEventPreProcessor, **was introduced in 0.0.14**
* Count method added to Elasticsearch response event

### 0.0.14

* Support for reading System property and System environment variable with StringTemplate
* Partition key for kinesis is now random if not specified
* Added http dispatcher configuration for Elasticsearch
* Retry configuration is now available for Elasticsearch
* Functions with side-effects are now returning Observable<Event> instead of Event to have make the API
simpler but this also makes it easier to evolve functions. **This breaks API, you have to change from map() 
to flatMap() for some functions**. Fixes #12
* Refactored the api of working with timers and delay (FixedDelay, LinearDelay, ExponentialDelay), they
are now part of the API and not part of the RetryStrategy. Simple to create custom timers for delay. Fixes #14
* Support for running with docker, including samples
* Added Java Grok sample to samples

### 0.0.13
* Enhanced http-server functionality
* JsonEvent has better support for numerical values
* Improved support for getting timestamps from ms and secs

### 0.0.12
* Fixed serious bug when handling elasticsearch error response
* Improved Conditionals to use instead of now @Deprecated ConditionalFunc1.

    computeIfAbsent / computeIfExists 
    computeIfTagExists / ifTagAbsent
    computeIfMatch / computeIfNotMatch
  

### 0.0.11

* Support for flatMap() when using ifExists/ifNotExists/ifMatch
* Fixed bug when reading elasticsearch response error and updated items
* Possible to extract a string from json field and decode to json
* Enhanced json support for "try" parse json, otherwise text_to_json
* Enhanced error handling of ElasticSearch to include message body
* Support for extracting a json body from a string field in an event.
* Support for ifNotExists(field).map(func) or .add(fields)
* Support for fingerprint (md5 checksum)
* Minor bugfixes

### 0.0.10
* Bulk API Client: Support updates by supplying document_id pattern
* Bulk API Client: StringTemplate patterns support for index field
* Metadata is now copied when creating events based on other events
* Project actually builds after clone...
* Lots of minor bugfixes

### Version 0.0.9

* Initial open source release
* Includes support for access to AWS Lambda Context object