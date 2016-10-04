package lumbermill;

import groovy.lang.Tuple2;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.SimpleGraphiteClient;
import lumbermill.internal.StringTemplate;
import lumbermill.net.api.Socket;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Graphite {

  private static final Logger LOGGER = LoggerFactory.getLogger (Graphite.class);


  public static Func1<JsonEvent, Observable<JsonEvent>> carbon(Map m) {
    return graphite (m);
  }


  /**
   * Will be removed in favour of carbon()
   */
  @Deprecated
  public static Func1<JsonEvent, Observable<JsonEvent>> graphite(Map m) {
    MapWrap config = MapWrap.of (m).assertExistsAny ("metrics");
    MapWrap metrics = MapWrap.of(config.get ("metrics"));

    final boolean dry = config.get ("dry", false);
    final String prefix = config.get ("prefix", "");

    String timestampField = config.get("timestamp_field", "@timestamp");
    String timestampPrecision = config.get("timestamp_precision", "ISO_8601");


    SimpleGraphiteClient simpleGraphiteClient = !dry ?
      SimpleGraphiteClient.create().carbonServer (config.get ("host", "localhost"), config.get ("port", 2003)) :
      SimpleGraphiteClient.create().carbonServer (new Socket () {
        @Override
        public boolean isConnected () {
          return true;
        }

        @Override
        public void close () {

        }

        @Override
        public Socket write (ByteString... data) {
          for (ByteString value : data) {
            LOGGER.info ("(dry) {}", value.utf8 ());
          }
          return this;
        }
      });

    List<Tuple2<StringTemplate, StringTemplate>> metricsAndValues = new ArrayList<> ();

    for (Object key : metrics.toMap ().keySet ()) {
        metricsAndValues.add (new Tuple2<> (StringTemplate.compile ((String)key),StringTemplate.compile (metrics.get ((String)key))));
    }

    return jsonEvent -> {
      for (Tuple2<StringTemplate, StringTemplate> tuple : metricsAndValues) {
        Optional<String> metric = tuple.getFirst ().format (jsonEvent);
        Optional<String> value = tuple.getSecond ().format (jsonEvent);
        if (metric.isPresent () && value.isPresent ()) {
          SimpleGraphiteClient.Metric graphiteMetric = SimpleGraphiteClient.Metric.create (prefix + metric.get (),
            Double.parseDouble (value.get ()));
          switch (timestampPrecision) {
            case "ISO_8601":
                graphiteMetric = graphiteMetric.withIsoTime (jsonEvent.valueAsString (timestampField));
              break;
            case "MILLIS":
              graphiteMetric.withTimeInMillis (jsonEvent.asLong (timestampField));
              break;
            case "SECONDS":
              graphiteMetric.withTimeInSecs (jsonEvent.asLong (timestampField));
              break;
            default: throw new IllegalStateException ("Unknown timestamp_precision " + timestampPrecision);
          }
          simpleGraphiteClient.save (graphiteMetric);

        } else {
          if (LOGGER.isTraceEnabled ()) {
            LOGGER.trace ("Entry is missing metric or value, not metric is stored");
          }

        }
      }
      return Observable.just (jsonEvent);
    };
  }


}
