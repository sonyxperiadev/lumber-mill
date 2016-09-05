# InfluxDB

### @Experimental

Consider this as functional but in progress. How this should be configured is not finalized.

## Configuration

Event data

```json
{
    "@timestamp"  : "ISO_DATE",
    "metric"      : "cpu",
    "value"       : 75.2,
    "device_name" : null
}
```


```groovy

import lumbermill.api.Codecs.*
import static lumbermill.influxdb.Influxdb

Codecs.JSON_OBJECT.from(json).toObservable()
  .flatMap ( influxdb.client (
        url         : 'http://host:port',
        user        : 'root',
        password    : 'root',
        db          : 'dbName',   // supports templating
        measurement : 'system',   // supports templating
        fields      : [
             // value should be the name of the field, template not supported to support correct type (WIP)
            '{metric}'  : 'value',
            '{metric2}' : 'value2',
        ],
        excludeTags : ['@timestamp', 'device_name', 'metric'],
        time        : 'time'                // Optional, override @timestamp field. First checks time and fallbacks to @timestamp if not exists
        precision   : TimeUnit.MILLISECONDS // Optional (default MS), precision of the time field
    )
).subscribe()

```

