package lumbermill;

import groovy.lang.Tuple2;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.SimpleGraphiteClient;
import lumbermill.internal.StringTemplate;
import lumbermill.internal.net.VertxTCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Graphite {

  private static final Logger LOGGER = LoggerFactory.getLogger (Graphite.class);


  public static Func1<JsonEvent, Observable<JsonEvent>> graphite(Map m) {
    MapWrap config = MapWrap.of (m).assertExistsAny ("metrics");
    MapWrap metrics = MapWrap.of(config.get ("metrics"));

    String timestampField = config.get("timestamp_field", "@timestamp");
    String timestampPrecision = config.get("timestamp_precision", "ISO_8601");

    SimpleGraphiteClient simpleGraphiteClient =
      new SimpleGraphiteClient (config.get ("host", "localhost"), config.get ("port", 2003));

    List<Tuple2<StringTemplate, StringTemplate>> metricsAndValues = new ArrayList<> ();


    for (Object key : metrics.toMap ().keySet ()) {
        metricsAndValues.add (new Tuple2<> (StringTemplate.compile ((String)key),StringTemplate.compile (metrics.get ((String)key))));
    }

    return jsonEvent -> {
      for (Tuple2<StringTemplate, StringTemplate> tuple : metricsAndValues) {
        Optional<String> metric = tuple.getFirst ().format (jsonEvent);
        Optional<String> value = tuple.getSecond ().format (jsonEvent);
        if (metric.isPresent () && value.isPresent ()) {
          SimpleGraphiteClient.Metric graphiteMetric = SimpleGraphiteClient.Metric.create (metric.get (),
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
