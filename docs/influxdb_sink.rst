InfluxDB
========

Stores metrics in influxdb.

**Usage**

*build.gradle*

.. code-block:: groovy

    compile 'com.sonymobile:lumbermill-influxdb:$version'


*Groovy script*

.. code-block:: json

    {
        "@timestamp"  : "ISO_DATE",
        "metric"      : "cpu",
        "avg"         : 75,
        "max",        : 98,
        "device_name" : null
    }


.. code-block:: groovy

    import lumbermill.api.Codecs.*
    import static lumbermill.influxdb.Influxdb

    Codecs.JSON_OBJECT.from(json).toObservable()
      .flatMap ( influxdb.client (
            url         : 'http://host:port', //Required
            user        : 'root',             //Required
            password    : 'root',             // Required
            db          : 'dbName',           // Required, supports templating
            measurement : '{metric}',         // Required, supports templating
            fields      : [
                 // value should be the name of the field, template not supported to support correct type (WIP)
                'avg'  : 'avg',
                'max'  : 'max',
            ],
            excludeTags : ['@timestamp', 'device_name', 'metric'],
            time        : 'time'                // Optional, override @timestamp field. First checks time and fallbacks to @timestamp if not exists
            precision   : TimeUnit.MILLISECONDS // Optional (default MS), precision of the time field
        )
    ).toBlocking()
    .subscribe()
