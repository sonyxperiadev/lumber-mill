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

   import lumbermill.api.Codecs
   import java.util.concurrent.TimeUnit
   import static lumbermill.Influxdb.influxdb

   client = influxdb.client (
           url         : 'http://influxdb:8086', //Required
           user        : 'root',             //Required
           password    : 'root',             // Required
           db          : 'testDb',           // Required, supports templating
           measurement : '{metric}',         // Required, supports templating
           fields      : [
                   // value should be the name of the field, template not supported to support correct type (WIP)
                   'avg'  : 'avg',
                   'max'  : 'max',
           ],
           excludeTags : ['@timestamp', 'message'],
           precision   : TimeUnit.MILLISECONDS // Optional (default MS), precision of the time field
   )

   rx.Observable.just(json)
           .buffer(2)
           .flatMap (client)
           .toBlocking()
           .subscribe()

