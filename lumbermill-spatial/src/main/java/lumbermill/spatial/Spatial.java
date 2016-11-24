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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Spatial {

    public static final String TARGET   = "target";
    public static final String DATABASE = "database";
    public static final String FIELDS   = "fields";
    public static final String SOURCE   = "source";

    /**
     * Factory method for simple integration into RxJava pipeline
     */
    public static Func1<JsonEvent, Observable<JsonEvent>> geoip(Map map) {

        GeoIP geoIP = create(map);

        return jsonEvent -> Observable.just(geoIP.decorate(jsonEvent));
    }


    private static GeoIP create(Map map) {
        MapWrap conf = MapWrap.of(map).assertExists(SOURCE);

        return GeoIP.Factory.create(conf.asString(SOURCE),
                conf.exists(TARGET) ? Optional.of(TARGET) : Optional.empty(),
                conf.exists(DATABASE) ? Optional.of(new File(conf.asString(DATABASE))) : Optional.empty(),
                conf.exists(FIELDS) ? Optional.of(conf.getObject(FIELDS)) : Optional.empty());
    }
}