# Lumber Mill Releases

### [Unreleased / master] Version 0.0.14-SNAPSHOT

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