.. Lumber-Mill documentation master file, created by
   sphinx-quickstart on Tue Nov  1 20:46:47 2016.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

API for Reactive Log Processing with focus on AWS!
=======================================

Lumber-Mill is designed for programmers/devops/SRE etc with a professional programming background but with an emergent
interest for devops, monitoring and log processing and who want total control of the event pipeline.

.. code-block:: groovy

    Observable call(Observable eventStream) {

        // Parse and de-normalize events
        eventStream.compose ( new CloudWatchLogsEventPreProcessor())

        .flatMap (
            grok.parse (
               field:        'message',
               pattern:      '%{AWS_LAMBDA_REQUEST_REPORT}'))

        .flatMap ( addField('type','cloudwatchlogs'))

        .flatMap ( fingerprint.md5())

        .buffer (100)

        .flatMap (
            AWS.elasticsearch.client (
                url:          '{es_url}',
                index_prefix: 'lumbermill-',
                type:         '{type}',
                region:       'eu-west-1',
                document_id:  '{fingerprint}'
            )
        )
    }



Contents:

.. toctree::
   :maxdepth: 1

   Home <self>
   five_minute_intro
   mutate
   sources
   sinks




Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`

