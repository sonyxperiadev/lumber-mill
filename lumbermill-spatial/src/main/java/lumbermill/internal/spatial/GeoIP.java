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
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 *
 */
public class GeoIP {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoIP.class);


    private static String DEFAULT_TARGET = "geoip";

    private static final List<String> DEFAULT_FIELDS = asList (
            "country_code2",
            "country_code3",
            "country_name",
            "continent_code",
            "continent_name",
            "city_name",
            "timezone",
            "locationOf",
            "latitude",
            "longitude"
    );


    /**
     * Fields that will be included in geoip node, Optional
     */
    private final List<String> fields;

    /**
     * GeoIP database
     */
    private final DatabaseReader reader;

    /**
     * Field where to read ip address in json
     */
    private final String sourceField;

    private final String targetField;


    /**
     *
     * @param sourceField Muse
     * @param reader
     * @param fields
     */
    private GeoIP(String sourceField, String targetField, DatabaseReader reader, List<String> fields) {
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.fields = fields;
        this.reader = reader;
    }

    public JsonEvent decorate(JsonEvent event) {
        if (!event.has(sourceField)) {
            return event;
        }
        String ip = event.valueAsString(sourceField);
        Optional<CityResponse> locationOptional = locationOf(ip);
        if (!locationOptional.isPresent()) {
            return event;
        }

        CityResponse location = locationOptional.get();

        ObjectNode geoIpNode = Json.OBJECT_MAPPER.createObjectNode();

        // Wrapper objects are never null but actual values can be null
        put(geoIpNode, "country_code2", location.getCountry().getIsoCode());
        put(geoIpNode, "country_code3", location.getCountry().getIsoCode());
        put(geoIpNode, "country_name", location.getCountry().getName());
        put(geoIpNode, "continent_code", location.getContinent().getCode());
        put(geoIpNode, "continent_name", location.getContinent().getName());
        put(geoIpNode, "city_name", location.getCity().getName());
        put(geoIpNode, "timezone", location.getLocation().getTimeZone());
        put(geoIpNode, "latitude", location.getLocation().getLatitude().doubleValue());
        put(geoIpNode, "longitude", location.getLocation().getLongitude().doubleValue());

        geoIpNode.set("location", Json.createArrayNode (
               location.getLocation().getLongitude().doubleValue(),
               location.getLocation().getLatitude().doubleValue()));

        event.unsafe().set(targetField, geoIpNode);
        return event;
    }

    private Optional<CityResponse> locationOf(String ip) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            CityResponse response = reader.city(ipAddress);
            if (!hasLocations(response)) {
                return Optional.empty();
            }
            return Optional.of(response);
        } catch (UnknownHostException e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("UnknownHostException: " + ip);
            }
        } catch (GeoIp2Exception e) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unexpected GeoIp2Exception: " + e.getMessage());
            }
        } catch (IOException e) {
            LOGGER.warn("Unexpected IOException", e);
        }
        return Optional.empty();
    }


    private boolean hasLocations(CityResponse location) {
        if (location == null || (location.getLocation().getLatitude() == null
                || location.getLocation().getLongitude() == null)) {
            return false;
        }
        return true;
    }

    private void put(ObjectNode node, String field, Double value) {
        if (fields.contains(field)) {
            if (value != null) {
                node.put(field, value);
            }
        }
    }

    private void put(ObjectNode node, String field, String value) {
        if (fields.contains(field)) {
            if (value != null) {
                node.put(field, value);
            }
        }
    }

    public static class Factory {

        public static GeoIP create(String source, Optional<String> target, Optional<File> path, Optional<List<String>> fields) {
            String theTarget          = target.isPresent() ? target.get() : DEFAULT_TARGET;
            DatabaseReader  theReader = path.isPresent() ? fromFile(path.get()) : fromClasspath();
            List<String> theFields    = fields.isPresent() ? fields.get() : DEFAULT_FIELDS;

            return new GeoIP(source, theTarget, theReader, theFields);
        }

        private static DatabaseReader fromFile(File path) {
            try {
                LOGGER.info("Opening GeoIP database from file {}", path);
                return new DatabaseReader.Builder(path).withCache(new CHMCache()).build();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to open Geo database file" + path, e);
            }
        }

        private static DatabaseReader fromClasspath() {
            try {
                LOGGER.info("Trying to open database GeoLite2-City.mmdb from classpath");
                URL resource = Thread.currentThread().getContextClassLoader().getResource("GeoLite2-City.mmdb");
                return new DatabaseReader.Builder(resource.openStream())
                        .withCache(new CHMCache()).build();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to open GeoLite2-City.mmdb database in classpath", e);
            }
        }
    }


}
