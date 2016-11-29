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
package lumbermill.internal.geospatial;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lumbermill.api.Codecs;
import lumbermill.api.JsonEvent;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 *
 * Since we do not bundle maxmind database tests are not executed on jenkins, they are run before push.
 * Consider how this can be solved properly
 */
public class GeoIPTest {

    @Ignore
    @Test
    public void test_geoip_all_fields() {
        GeoIP geoIP = GeoIP.Factory.create("client_ip", Optional.of("geoip"), Optional.of(new File("/tmp/GeoLite2-City.mmdb")), Optional.empty());
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello").put("client_ip", "37.139.156.40");
        geoIP.decorate(event);

        JsonNode geoip = event.unsafe().get("geoip");
        assertThat(geoip.get("country_code2").asText()).isEqualTo("SE");
        assertThat(geoip.get("country_code3").asText()).isEqualTo("SE");
        assertThat(geoip.get("timezone").asText()).isEqualTo("Europe/Stockholm");
        assertThat(geoip.get("continent_code").asText()).isEqualTo("EU");
        assertThat(geoip.get("continent_name").asText()).isEqualTo("Europe");
        assertThat(geoip.get("country_name").asText()).isEqualTo("Sweden");
        assertThat(geoip.get("city_name").asText()).isEqualTo("Sodra Sandby");
        assertThat(geoip.get("longitude").asDouble()).isEqualTo(13.3333);
        assertThat(geoip.get("latitude").asDouble()).isEqualTo(55.7167);
        ArrayNode location = (ArrayNode)geoip.get("location");
        assertThat(location.get(0).asDouble()).isEqualTo(13.3333);
        assertThat(location.get(1).asDouble()).isEqualTo(55.7167);
        System.out.println(event);
    }

    @Ignore
    @Test
    public void test_geoip_some_fields() {
        GeoIP geoIP = GeoIP.Factory.create("client_ip",
                Optional.of("geoip2"),
                Optional.of(new File("/tmp/GeoLite2-City.mmdb")),
                Optional.of(asList("timezone", "location")));

        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello").put("client_ip", "37.139.156.40");
        geoIP.decorate(event);

        JsonNode geoip = event.unsafe().get("geoip2");
        assertThat(geoip.get("country_code2")).isNull();
        assertThat(geoip.get("country_code3")).isNull();
        assertThat(geoip.get("timezone").asText()).isEqualTo("Europe/Stockholm");
        assertThat(geoip.get("continent_code")).isNull();
        assertThat(geoip.get("continent_name")).isNull();
        assertThat(geoip.get("country_name")).isNull();
        assertThat(geoip.get("city_name")).isNull();
        assertThat(geoip.get("longitude")).isNull();
        assertThat(geoip.get("latitude")).isNull();
        ArrayNode location = (ArrayNode)geoip.get("location");
        assertThat(location.get(0).asDouble()).isEqualTo(13.3333);
        assertThat(location.get(1).asDouble()).isEqualTo(55.7167);
        System.out.println(event);
    }


}
