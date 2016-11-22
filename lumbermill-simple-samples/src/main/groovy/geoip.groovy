import lumbermill.api.Codecs

import static lumbermill.Core.console
import static lumbermill.spatial.Spatial.geoip

/*
  This requires the database to exist in /tmp directory
  Run the following to download and extract

  (cd /tmp && wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz && gunzip GeoLite2-City.mmdb.gz)
*/

Codecs.TEXT_TO_JSON.from("Hello").put("client_ip", "37.139.156.40")
.toObservable()
.flatMap(
    geoip (
            'field': 'client_ip',
            'path' : '/srv/GeoLite2-City.mmdb'
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