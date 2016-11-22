/*
 * Copyright 2016 Sony Mobile Communications, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package lumbermill.internal.spatial;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import lumbermill.api.JsonEvent;
import lumbermill.internal.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class GeoIP {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIP.class);

    private final DatabaseReader reader;

    private final String sourceField;

    public GeoIP(String sourceField, File path) {
        this.sourceField = sourceField;
        try {
            reader = new DatabaseReader.Builder(path).withCache(new CHMCache()).build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open Geo database file" + path, e);
        }
    }

    public GeoIP(String sourceField){
        try {
            this.sourceField = sourceField;
            URL resource = Thread.currentThread().getContextClassLoader().getResource("GeoLite2-City.mmdb");
            reader = new DatabaseReader.Builder(resource.openStream())
                    .withCache(new CHMCache()).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private CityResponse location(String ip) {

        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            return reader.city(ipAddress);
        } catch (UnknownHostException e) {
            return null;
        } catch (GeoIp2Exception e) {

            return null;
        } catch (IOException e) {
            return null;
        }
    }


    public JsonEvent decorate(JsonEvent event) {
        String ip = event.valueAsString(sourceField);
        CityResponse location = location(ip);
        if (location == null) {
            return event;
        }
        ObjectNode geoIpNode = Json.OBJECT_MAPPER.createObjectNode();
        geoIpNode.put("country_code2", location.getCountry().getIsoCode());
        geoIpNode.put("continent_code", location.getContinent().getCode());
        geoIpNode.put("continent", location.getContinent().getName());
        geoIpNode.put("country_name", location.getCountry().getName());
        if (location.getCity() != null) {
            geoIpNode.put("city_name", location.getCity().getName());
        }
        if (location.getLocation() != null) {
            ArrayNode locationNode = Json.createArrayNode(
                    location.getLocation().getLongitude().doubleValue(),
                    location.getLocation().getLatitude().doubleValue());
            geoIpNode.put("longitude", location.getLocation().getLongitude().doubleValue());
            geoIpNode.put("latitude",  location.getLocation().getLatitude().doubleValue());
            geoIpNode.set("location", locationNode);
        }

        event.unsafe().set("geoip", geoIpNode);
        return event;
    }
}
