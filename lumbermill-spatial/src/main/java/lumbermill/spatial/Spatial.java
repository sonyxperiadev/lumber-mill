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
package lumbermill.spatial;


import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.spatial.GeoIP;
import rx.Observable;
import rx.functions.Func1;

import java.io.File;
import java.util.Map;

public class Spatial {

    public static Func1<JsonEvent, Observable<JsonEvent>> geoip(Map map) {

        MapWrap conf = MapWrap.of(map).assertExists("field");
        String field = conf.asString("field");

        final GeoIP geoIP = conf.exists("path") ? new GeoIP(field, new File(conf.asString("path"))) : new GeoIP(field);

        return jsonEvent -> Observable.just(geoIP.decorate(jsonEvent));
    }

}
