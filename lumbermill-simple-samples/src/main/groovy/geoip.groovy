import lumbermill.api.Codecs

import rx.Observable
import static lumbermill.Core.console
import static lumbermill.geospatial.GeoSpatial.geoip

/*
   Run this with 'docker run lifelog/lumber-mill:latest geoip.groovy'
*/

Observable.just(
        Codecs.TEXT_TO_JSON.from("Hello").put("client_ip", "37.139.156.40"),
        Codecs.TEXT_TO_JSON.from("World").put("client_ip", "37.139.156.40"))
.flatMap(
    geoip (
            'source': 'client_ip',
            'database' : '/srv/GeoLite2-City.mmdb'
    )
)
.doOnNext(console.stdout())
.subscribe()

/*
 Output should be

 {
  "message" : "Hello",
  "@timestamp" : "2016-11-22T17:09:53.948+01:00",
  "client_ip" : "37.139.156.40",
  "geoip" : {
    "country_code2" : "SE",
    "continent_code" : "EU",
    "continent" : "Europe",
    "country_name" : "Sweden",
    "city_name" : "Sodra Sandby",
    "longitude" : 13.3333,
    "latitude" : 55.7167,
    "location" : [ 13.3333, 55.7167 ]
  }
}

 */