Local Filesystem
================

The fs support is designed mainly to support one-time read of files which might
be temporary files downloaded from S3 or one-time jobs when recursively iterating a
filesystem.

*It does not support tail, if you need that there are better solutions**

Default codec is Codecs.TEXT_TO_JSON

**Read a file once**

This will create the source Observable and does not "hook" into an existing pipeline

.. code-block:: groovy

    import lumbermill.api.Codecs

    import static lumbermill.Core.file

    file.readFileAsLines (
            file:  '/tmp/afile',
            codec : Codecs.TEXT_TO_JSON)
    .filter( keepWhen( '{message}'.contains('ERROR') )
    .doOnNext( console.stdout('Errors: {message}') )
    .subscribe()

**Read each line in an existing pipeline**

If you i.e have downloaded a file from S3 or iterating a number of files you can use the
*file.lines()* to read each line and return as Observables. This also takes the codec
parameter if required.

.. code-block:: groovy

    .flatMap (
        file.lines(file: '{s3_download_path}')
    )
