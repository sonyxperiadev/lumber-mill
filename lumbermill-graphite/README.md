# Graphite metrics

Extract graphite metrics from your data. 

## @Experimental


```groovy

Codecs.TEXT_TO_JSON.from("hello")
    .put("@metric", "hits.count")
    .put("@value", 5)
    .toObservable()
    .flatMap (
        graphite (
            host: 'localhost,                             // Optional (localhost)          
            port: 2003,                                   // Optional (2003)
            timestamp_field: '@timestamp',                // Optional (@timestamp)
            timestamp_precision: 'ISO_8601',              // Optional (ISO_8601). Supports 'MILLIS' and 'SECONDS'
            metrics : [                                   // At least one metric is required
              'counters.{@metric}' : '{@value}',          // If either @metric or @value is missing no metric is stored
              'counters.duplicate.{@metric}' : '{@value}'
            ]
        )
).toBlocking().subscribe()
```