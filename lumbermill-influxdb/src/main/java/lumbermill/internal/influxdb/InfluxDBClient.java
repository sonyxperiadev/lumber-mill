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


public class InfluxDBClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxDBClient.class);

    private static final List<String> DEFAULT_EXCLUDED_TAGS = asList("@timestamp", "message", "@version");

    public static InfluxDBClient prepareForTest(InfluxDBClient.Factory factory) {
        return new InfluxDBClient (factory);
    }

    public InfluxDBClient () {
        dbFactory = new DefaultFactory();
    }

    private InfluxDBClient (Factory factory) {
        this.dbFactory = factory;
    }

    private final InfluxDBClient.Factory dbFactory;


    public Func1<List<JsonEvent>, Observable<List<JsonEvent>>> client(Map map) {

        final MapWrap config                     = MapWrap.of(map).assertExists("fields", "db", "url", "user", "password");
        final StringTemplate measurementTemplate = config.asStringTemplate("measurement");
        final StringTemplate dbTemplate          = config.asStringTemplate("db");

        final InfluxDB influxDB = dbFactory.create(config);

        return events -> {

            Observable.from(events)
                    .groupBy(e -> dbTemplate.format(e).get())
                    .doOnNext(byDatabase -> ensureDatabaseExists (influxDB, byDatabase))
                    .doOnNext(byDatabase ->
                        byDatabase
                                .flatMap(jsonEvent -> buildPoint(config, measurementTemplate, jsonEvent))
                                .buffer(config.get("flushSize", 100))
                                .flatMap(points -> toBatchPoints (byDatabase, points))
                                .doOnNext(batchPoints -> influxDB.write(batchPoints))
                                .subscribe()
                    ).subscribe();
            return Observable.just(events);
        };
    }

    private Observable<BatchPoints> toBatchPoints (GroupedObservable<String, JsonEvent> byDatabase, List<Point> points) {
        return Observable.just(BatchPoints.database(ensureDatabaseNameIsValid (byDatabase.getKey ()))
               .points(points.toArray(new Point[0])).build());
    }

    private String ensureDatabaseNameIsValid (String dbName) {
        return dbName.replaceAll("[^A-Za-z0-9]", "");
    }

    /**
   * TODO - Add a cache of databases that evicts names after a certain interval but removes an extra HTTP call for each invocation.
   */
  private void ensureDatabaseExists (InfluxDB influxDB, GroupedObservable<String, JsonEvent> byDatabase) {
        influxDB.createDatabase(ensureDatabaseNameIsValid (byDatabase.getKey ()));
    }

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
        Point.Builder measurement =
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
                measurement.addField(formattedFieldNameOptional.get(), node.asDouble());
            } else if (node.isBoolean()) {
                measurement.addField(formattedFieldNameOptional.get(), node.asBoolean());
            } else  {
                measurement.addField(formattedFieldNameOptional.get(), node.asText());
            }
        }

        Iterator<String> stringIterator = jsonEvent.unsafe().fieldNames();
        while (stringIterator.hasNext()) {
            String next = stringIterator.next();
            if (!excludeTags.contains(next)) {
                measurement.tag(next, jsonEvent.valueAsString(next));
            }
        }

        Optional<String> timeField = config.getIfExists("time");
        TimeUnit precision         = config.get("precision", TimeUnit.MILLISECONDS);

        // Override @timestamp with a ISO_8601 String or a numerical value
        if (timeField.isPresent() && jsonEvent.has(config.asString("time"))) {

            if (jsonEvent.unsafe().get(timeField.get()).isTextual()) {
                measurement.time(ZonedDateTime.parse(jsonEvent.valueAsString("@timestamp"),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli(),
                        precision);
            } else {
                measurement.time(jsonEvent.asLong(timeField.get()), precision);
            }
        } else {
            // If not overriden, check if timestamp exists and use that
            if (jsonEvent.has("@timestamp")) {
                measurement.time(ZonedDateTime.parse(jsonEvent.valueAsString("@timestamp"),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli(),
                        precision);
            }
        }

        if (!addedAtLeastOneField) {
            LOGGER.debug("Could not create a point since no fields where added");
            return Observable.empty();
        }
        return Observable.just(measurement.build());
    }

    public  interface Factory {
        InfluxDB create (MapWrap mapWrap);
    }

    private static class DefaultFactory implements InfluxDBClient.Factory {

        private final static Map<String, InfluxDB> databases = new HashMap<> ();

        @Override
        public InfluxDB create(MapWrap config) {
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
