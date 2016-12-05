Enriching functions
===================

Functions used in the pipeline to mutate/enrich the event contents.

Unless specified, functions are part of the core module which is used by depending on the core module
and importing all methods on the lumbermill.Core class.

.. code-block:: groovy

    compile 'com.sonymobile:lumbermill-core:$version'

    import static lumbermill.Core.*


Add / Remove / Rename
---------------------

.. code-block:: groovy

    o.flatMap ( addField('name', 'string'))
    o.flatMap ( addField('name', 10))
    o.flatMap ( addField('name', true))
    o.flatMap ( addField('name', 10.8))

    o.flatMap( remove('field'))
    o.flatMap( remove('field1', 'field2'))

    o.flatMap ( rename (from: 'source', to: 'target'))

Base64
------

Base64 encodes and decodes the contents of an Event and returns a lumbermill.api.BytesEvent

.. code-block:: groovy

    o.flatMap ( base64.encode())

    o.flatMap ( base64.decode())


Fingerprint / Checksum
----------------------

Adds a fingerprint based on either the complete payload or based on on or more fields (supports pattern).

It is up to the user to create the source string to be used as fingerprint. Best practice to separate each
'word' with a char, like a pipe (|) char to prevent any unexpected behaviour.
Read more at https://github.com/google/guava/wiki/HashingExplained.


.. code-block:: groovy

    o.flatMap( fingerprint.md5('{@timestamp}|{message}'))

    // Raw payload
    o.flatMap( fingerprint.md5())

    // To access the fingerprint, use field 'fingerprint'
    o.doOnNext( console.stdout('Fingerprint was {fingerprint}'))


Compression
-----------

Support for gzip and zlib.

*Zlib support for file compression/decompression is not finished, only for event contents*

Example of file compression/decompression can be a reference to an S3 file that is compressed
and must be decompressed before usage. Or a local file reference that must be compressed before
put back on S3.

.. code-block:: groovy

    // Compress a file
    o.flatMap ( gzip.compress (
        file: 'fileName', // Supports pattern
        output_field: 'gzip_path_compressed' // Optional, defaults to gzip_path_compressed
    ))

    // Decompress a file
    o.flatMap ( gzip.decompress (
        file: 'fileName', // Supports pattern
        output_field: 'gzip_path_decompressed' // Optional, defaults to gzip_path_decompressed
    )

    // Decompress a payload
    o.flatMap ( gzip.decompress())
    o.flatMap ( zlib.decompress())

    // Compress a payload
    o.flatMap ( gzip.compress())
    o.flatMap ( zlib.compress())


Timestamps
----------

Helps out converting different times to *@timestamp: ISO_8601.*

.. code-block:: groovy

    // Add timestamp field now
    o.flatMap( timestampNow())

    // Timestamp from @timestamp that contains time in seconds into @timestamp
    o.flatMap( timestampFromSecs())

    // Timestamp from a field that contains time in seconds into @timestamp
    o.flatMap( timestampFromSecs('fieldWithTime'))

    // Timestamp from a field that contains time in seconds into another field
    o.flatMap( timestampFromSecs('fieldWithTime', 'targetFieldWithTime'))

    // Timestamp from @timestamp that contains time in millis into @timestamp
    o.flatMap( timestampFromMs())

    // Timestamp from a field that contains time in millis into @timestamp
    o.flatMap( timestampFromMs('fieldWithTime'))

    // Timestamp from a field that contains time in millis into another field
    o.flatMap( timestampFromMs('fieldWithTime', 'targetFieldWithTime'))

Conditionals
------------

Currently, the support for conditionals is limited but it is WIP. It is currently done by using one of the compute* methods.

The conditional functions can:

 - return a function
 - invoke a function
 - invoke multiple functions

.. code-block:: groovy

    // Execute If a tag exists
    computeIfTagExists ('tagName');

    // Execute If a tag does not exists
    computeIfTagIsAbsent ('tagName');

    // Execute If a regex match a field
    computeIfMatch ('message', '<regex>');

    // Execute If a regex does not match a field
    computeIfNotMatch ('message', '<regex>');

    // Execute If a field exists
    computeIfExists('fieldName')

    //Execute if a field does not exist
    computeIfAbsent('fieldName')

    // This will create a fingerprint unless the field 'fingerprint' already exists
    o.flatMap ( computeIfAbsent('fingerprint') {
        fingerprint.md5()
    })

Filters
-------

RxJava provides the *observable.filter()* operation that can be used to keep or skip data. Lumber-Mill provides two
functions that can be used together with filter.

The expression uses JavaScript, so it must be valid javascript and must return a boolean value.

.. code-block:: groovy

    // Strings
    o.filter( keepWhen("'{name}' == 'Johan'"))

    // Numbers
    o.filter( skipWhen("{age} == 99"))

    // Boolean
    o.filter( skipWhen("{isHappy} == false)")

    // Array
    o.filter( keepWhen("{tags}.contains('Johan')")


Grok
----

Grok is one of the most powerful functions in lumbermill and it works "almost" in the same way as in logstash.
Lumber-Mill is bundled with the same grok patterns as Logstash is, plus a few more AWS related patterns.

This sample expects an AWS ELB file to be processed.

.. code-block:: groovy

    o.flatMap( grok.parse (
        field: 'message',
        pattern: '%{AWS_ELB_LOG}',
        tagOnFailure: true,        // Optional, defaults to true
        tag: '_grokparsefailure'   // Optional, defaults to _grokparsefailure
    ))

GeoIP
-----

This comes as a separate module *lumbermill-geospatial* and it also requires you to download the database to use.

To prevent classpath issues, you must exclude jackson dependencies when depending on this module.

.. code-block:: groovy

    compile ('com.sonymobile:lumbermill-geospatial:$version') {
            exclude group: 'com.fasterxml.jackson.core'
            exclude group: 'com.fasterxml.jackson.databind'
            exclude group: 'com.fasterxml.jackson.annotations'
     }

.. code-block:: groovy

    o.flatMap (
        geoip (
            'source' : 'client_ip', // Required - if field does not exist it simply will not add any geo info
            'target' : 'geoip',     // Optional - defaults to 'geoip'
            'path'   : '/tmp/GeoLite2-City.mmdb', // Optional, but if not supplied GeoLite2-City.mmdb must be found on classpath
            'fields' : ['country_code2', 'location'] // Optional, defaults to all fields
        )
    )

Important, the GeoLite2-City.mmdb **MUST** be downloaded and imported from the project
that depends on this module, the database in **NOT** included in the distribution.

.. code-block:: shell

    wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
    gunzip GeoLite2-City.mmdb.gz


The database file can be opened from classpath if you make it available there, and this
is default behaviour.

.. code-block:: shell

    mv GeoLite2-City.mmdb your_project/src/main/resources


Or it can be located somewhere on the filesystem

.. code-block:: shell

    mv GeoLite2-City.mmdb /tmp

.. code-block:: groovy

    geoip (field: 'client_ip', path: '/tmp/GeoLite2-City.mmdb.gz')


**Docker**

Simply prepare the image with the maxmind database

.. code-block:: docker

    WORKDIR /srv
    RUN wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
    RUN gunzip GeoLite2-City.mmdb.gz

And use it from code

.. code-block:: groovy

    geoip (
        'source' : 'client_ip',
        'path'   : '/srv/GeoLite2-City.mmdb'
    )