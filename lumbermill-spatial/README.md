# Spatial stuff


## GeoIP

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

