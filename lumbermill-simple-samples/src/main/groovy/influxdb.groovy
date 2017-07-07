import lumbermill.api.Codecs

import java.util.concurrent.TimeUnit



client = lumbermill.Influxdb.influxdb.client (
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


json = Codecs.TEXT_TO_JSON.from("apa")
        .put('metric','height')
        .put('avg', 50)
        .put('max', 100)

json2 = Codecs.TEXT_TO_JSON.from("apa")
        .put('metric','weight')
        .put('avg', 50)
        .put('max', 100)


rx.Observable.just(json, json2)
        .buffer(2)
        .flatMap (client)
        .toBlocking()
        .subscribe()
