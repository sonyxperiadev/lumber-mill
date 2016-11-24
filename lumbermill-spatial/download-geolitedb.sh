#!/usr/bin/env bash

mkdir -p build
wget http://geolite.maxmind.com/download/geoip/database/GeoLite2-City.mmdb.gz
(mkdir -p build/classes/main && mv GeoLite2-City.mmdb.gz build/classes/main && cd build/classes/main && gunzip GeoLite2-City.mmdb.gz)
