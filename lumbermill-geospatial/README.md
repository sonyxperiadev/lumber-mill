# Spatial stuff

So far only GeoIP is supported but there is some more stuff in the pipeline

## GeoIP

This tries to be as similar to logstash-geoip-plugin as possible.

### Use

```groovy

observable.flatMap (
    geoip (
        'source' : 'client_ip', // Required - if field does not exist it simply will not add any geo info
        'target' : 'geoip',     // Optional - defaults to 'geoip'
        'path'   : '/tmp/GeoLite2-City.mmdb', // Optional, but if not supplied GeoLite2-City.mmdb must be found on classpath
        'fields' : ['country_code2', 'location'] // Optional, defaults to all fields
    )    
)
```

#### Sample

* Original Json

```json

{
  "message" : "Hello",
  "@timestamp" : "2016-11-25T12:26:30.414+01:00",
  "client_ip" : "37.139.156.40"
}
```


* GeoIP configuration

```
observable.flatMap (
    geoip (
        'source' : 'client_ip', 
        'path'   : '/tmp/GeoLite2-City.mmdb'
    )
).doOnNext(consle.stdout())
```

* Will output

```groovy
{
  "message" : "Hello",
  "@timestamp" : "2016-11-25T12:26:30.414+01:00",
  "client_ip" : "37.139.156.40",
  "geoip" : {
    "country_code2" : "SE",
    "country_code3" : "SE",
    "country_name" : "Sweden",
    "continent_code" : "EU",
    "continent_name" : "Europe",
    "city_name" : "Sodra Sandby",
    "timezone" : "Europe/Stockholm",
    "latitude" : 55.7167,
    "longitude" : 13.3333,
    "location" : [ 13.3333, 55.7167 ]
  }
}
```


### Build
You must exclude 2.8.x version of jackson to make it work with other parts
of lumbermill since they use an older version.

```groovy
compile ('com.sonymobile:lumbermill-spatial:$version') {
        exclude group: 'com.fasterxml.jackson.core'
        exclude group: 'com.fasterxml.jackson.databind'
        exclude group: 'com.fasterxml.jackson.annotations'
    }
```


### Download and use maxmind database
This product includes GeoLite2 data created by MaxMind, available from
<a href="http://www.maxmind.com">http://www.maxmind.com</a>.

Important, the GeoLite2-City.mmdb **MUST** be downloaded and imported from the project
that depends on this module, the database in **NOT** included in the distribution.

```bash
wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
gunzip GeoLite2-City.mmdb.gz
```

The database file can be opened from classpath if you make it avaible there, this
is default behaviour.

```bash
mv GeoLite2-City.mmdb your_project/src/main/resources
```
```groovy
geoip (field: 'client_ip')
```

Or it can be located somewhere on the filesystem

```bash
mv GeoLite2-City.mmdb /tmp
```
```groovy
geoip (field: 'client_ip', path: '/tmp/GeoLite2-City.mmdb.gz')
```

#### Docker

Simply prepare the image with the maxmind database

```
WORKDIR /srv
RUN wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
RUN gunzip GeoLite2-City.mmdb.gz
```
And use it from code

```groovy
geoip (
    'source' : 'client_ip', 
    'path'   : '/srv/GeoLite2-City.mmdb'
)
```