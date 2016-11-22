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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lumbermill.api.Codecs;
import lumbermill.api.JsonEvent;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

//@Ignore
public class GeoIPTest {


    // Ignored since we do not have database on travis
    @Ignore
    @Test
    public void test_geoip_no_database_found() {
        GeoIP geoIP = new GeoIP("client_ip");
        JsonEvent event = Codecs.TEXT_TO_JSON.from("Hello").put("client_ip", "37.139.156.40");
        geoIP.decorate(event);

        JsonNode geoip = event.unsafe().get("geoip");
        assertThat(geoip.get("country_code2").asText()).isEqualTo("SE");
        assertThat(geoip.get("continent_code").asText()).isEqualTo("EU");
        assertThat(geoip.get("continent").asText()).isEqualTo("Europe");
        assertThat(geoip.get("country_name").asText()).isEqualTo("Sweden");
        assertThat(geoip.get("city_name").asText()).isEqualTo("Sodra Sandby");
        assertThat(geoip.get("longitude").asDouble()).isEqualTo(13.3333);
        assertThat(geoip.get("latitude").asDouble()).isEqualTo(55.7167);
        ArrayNode location = (ArrayNode)geoip.get("location");
        assertThat(location.get(0).asDouble()).isEqualTo(13.3333);
        assertThat(location.get(1).asDouble()).isEqualTo(55.7167);
    }
}
