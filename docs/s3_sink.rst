S3
==

All S3 functions supports the config field *roleArn* to be able to assume a different role.

.. code-block:: groovy

    o.flatMap (
        s3.download (
            roleArn: 'the_role_arn_to_assume')
        )

S3 Download / Get
-----------------

**Download to filesystem**

I.e, an S3 event that you receive from an AWS Lambda will contain the *bucket* and *key* to that file so downloading
this locally is simple. The sample below will download it as a temp file on local disk and add the field 's3_download_path'
to the event.

.. code-block:: groovy

    observable.flatMap (
            s3.download (
                bucket: '{bucket_name}',
                key: '{key}',
                remove: true,
                output_field: 's3_download_path' // Optional, defaults to s3_download_path
            )
        ).doOnNext(console.stdout('File downloaded with filename {s3_download_path}'))

**Download as BytesEvent**

Similar to the example above but instead a BytesEvent with the complete contents of the S3 file is downloaded.

.. code-block:: groovy

    observable.flatMap (
            s3.get (
                bucket: '{bucket_name}',
                key: '{key}',
                codec: Codecs.BYTES // Optional, defaults to bytes
            )
        ).doOnNext{Event e -> println 'Size in bytes: ' + e.raw().size()}


S3 Put
------

S3 Put takes a reference to a file and puts it on S3. The example below takes the downloaded file and makes a copy with
the appended suffix '.copy'.

.. code-block:: groovy

    .flatMap (
            s3.put (
                bucket: '{bucket}',
                key   : '{key}.copy',
                file  : '{s3_download_path}'
            )
        )