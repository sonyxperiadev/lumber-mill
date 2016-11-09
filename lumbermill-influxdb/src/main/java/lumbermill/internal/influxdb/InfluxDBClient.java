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
package lumbermill.internal.influxdb;


import com.fasterxml.jackson.databind.JsonNode;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.StringTemplate;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.GroupedObservable;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static lumbermill.internal.Concurrency.ioJob;

/**
 * Based on the configuration it builds Influxdb BatchPoints and stores in Influxdb.
 * All IO is done async.
 */
public class InfluxDBClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDBClient.class);

    private static final List<String> DEFAULT_EXCLUDED_TAGS = asList("@timestamp", "message", "@version");

    public static InfluxDBClient prepareForTest(InfluxDBClient.Factory factory) {
        return new InfluxDBClient (factory);
    }

    private final InfluxDBClient.Factory dbFactory;

    public InfluxDBClient (){this(new DefaultFactory());
    }

    private InfluxDBClient (Factory factory) {
        this.dbFactory = factory;
    }

    /**
     * Creates a function that can be invoked with flatMap().
     * Use buffer(n) to decide how large each batch should be
     *
     * @param map - is the config
     */
    public Func1<List<JsonEvent>, Observable<List<JsonEvent>>> client(Map map) {

        final MapWrap config                     = MapWrap.of(map).assertExists("fields", "db", "url", "user", "password");
        final StringTemplate measurementTemplate = config.asStringTemplate("measurement");
        final StringTemplate dbTemplate          = config.asStringTemplate("db");

        final InfluxDB influxDB = dbFactory.createOrGet(config);

        return events ->

            Observable.from(events)
                    .groupBy(e -> dbTemplate.format(e).get())
                    .flatMap(byDatabase -> ensureDatabaseExists (influxDB, byDatabase))
                    .doOnNext(byDatabase ->
                        byDatabase
                                .flatMap(jsonEvent -> buildPoint(config, measurementTemplate, jsonEvent))
                                .buffer(config.get("flushSize", 100))
                                .map(points -> toBatchPoints (byDatabase, points))
                                .flatMap(batchPoints -> save(batchPoints, influxDB))
                    )
                    .flatMap(o -> Observable.just(events));
    }

    /**
     * Saves BatchPoints on IO thread
     */
    private Observable<BatchPoints> save(BatchPoints batchPoints, InfluxDB db) {
        return  ioJob(() -> {
            db.write(batchPoints);
            return batchPoints;
        });
    }

    /**
     * Converts a list of Points to BatchPoints
     */
    private BatchPoints toBatchPoints (GroupedObservable<String, JsonEvent> byDatabase, List<Point> points) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Storing batch of {} in db {}", points.size(), byDatabase.getKey());
        }
        return BatchPoints.database(ensureDatabaseNameIsValid (byDatabase.getKey ()))
               .points(points.toArray(new Point[0])).build();
    }

    /**
     * Removes illegal chars from db name
     */
    private String ensureDatabaseNameIsValid (String dbName) {
        return dbName.replaceAll("[^A-Za-z0-9]", "");
    }

    /**
     * Invokes createOrGet database command to make sure that the database exists
     *
     * * TODO - Add a cache of databases that evicts names after a certain interval but removes an extra HTTP call for each invocation.
   */
  private Observable<GroupedObservable<String, JsonEvent>> ensureDatabaseExists (InfluxDB influxDB, GroupedObservable<String, JsonEvent> byDatabase) {
      if (LOGGER.isTraceEnabled()) {
          LOGGER.trace("Ensuring db exists: {}", byDatabase.getKey());
      }
      return ioJob(() -> {
          influxDB.createDatabase(ensureDatabaseNameIsValid (byDatabase.getKey ()));
          return byDatabase;
      });
    }

    /**
     * Creates  Points based on the event and config
     */
    private static Observable<Point> buildPoint(MapWrap config, StringTemplate measurementTemplate, JsonEvent jsonEvent) {

        final MapWrap fieldsConfig = MapWrap.of(config.get("fields"));
        final List<String> excludeTags = config.get("excludeTags", DEFAULT_EXCLUDED_TAGS);

        // One field is required, otherwise the point will not be created
        boolean addedAtLeastOneField = false;
        Optional<String> measurementOptional = measurementTemplate.format(jsonEvent);
        if (!measurementOptional.isPresent()) {
            LOGGER.debug("Failed to extract measurement using {}, not points will be created", measurementTemplate.original());
            return Observable.empty();
        }
        Point.Builder pointBuilder =
                Point.measurement(measurementOptional.get());


        for (Object entry1: fieldsConfig.toMap().entrySet()) {
            Map.Entry<String, String> entry = (Map.Entry)entry1;
            StringTemplate fieldName = StringTemplate.compile(entry.getKey());
            String valueField = entry.getValue();

            JsonNode node = jsonEvent.unsafe().get(valueField);
            if (node == null) {
                LOGGER.debug("Failed to extract any field for {}", valueField);
                continue;
            }

            Optional<String> formattedFieldNameOptional = fieldName.format(jsonEvent);
            if (!formattedFieldNameOptional.isPresent()) {
                LOGGER.debug("Failed to extract any field for {}", fieldName.original());
                continue;
            }

            addedAtLeastOneField = true;

            if (node.isNumber()) {
                pointBuilder.addField(formattedFieldNameOptional.get(), node.asDouble());
            } else if (node.isBoolean()) {
                pointBuilder.addField(formattedFieldNameOptional.get(), node.asBoolean());
            } else  {
                pointBuilder.addField(formattedFieldNameOptional.get(), node.asText());
            }
        }

        Iterator<String> stringIterator = jsonEvent.unsafe().fieldNames();
        while (stringIterator.hasNext()) {
            String next = stringIterator.next();
            if (!excludeTags.contains(next)) {
                pointBuilder.tag(next, jsonEvent.valueAsString(next));
            }
        }

        Optional<String> timeField = config.getIfExists("time");
        TimeUnit precision         = config.get("precision", TimeUnit.MILLISECONDS);

        // Override @timestamp with a ISO_8601 String or a numerical value
        if (timeField.isPresent() && jsonEvent.has(config.asString("time"))) {

            if (jsonEvent.unsafe().get(timeField.get()).isTextual()) {
                pointBuilder.time(ZonedDateTime.parse(jsonEvent.valueAsString("@timestamp"),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli(),
                        precision);
            } else {
                pointBuilder.time(jsonEvent.asLong(timeField.get()), precision);
            }
        } else {
            // If not overriden, check if timestamp exists and use that
            if (jsonEvent.has("@timestamp")) {
                pointBuilder.time(ZonedDateTime.parse(jsonEvent.valueAsString("@timestamp"),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli(),
                        precision);
            }
        }

        if (!addedAtLeastOneField) {
            LOGGER.debug("Could not create a point since no fields where added");
            return Observable.empty();
        }

        Point point = pointBuilder.build();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Point to be stored {}", point.toString());
        }
        return Observable.just(point);
    }

    public  interface Factory {
        InfluxDB createOrGet(MapWrap mapWrap);
    }

    private static class DefaultFactory implements InfluxDBClient.Factory {

        private final static Map<String, InfluxDB> databases = new HashMap<> ();

        @Override
        public InfluxDB createOrGet(MapWrap config) {
            config.assertExists("url", "user", "password");
            LOGGER.info("Connecting to InfluxDB {}, user: {}",config.asString("url"), config.asString("user") );
            if (databases.containsKey (key (config))) {
                return databases.get (key (config));
            }
            InfluxDB influxDB =  InfluxDBFactory.connect(config.asString("url"),
              config.asString("user"), config.asString("password"));
            databases.put (key (config), influxDB);
            return influxDB;
        }

        private String key(MapWrap config) {
            return format("%s:%s", config.asString ("url"), config.asString ("user"));
        }
    }
}
