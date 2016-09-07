package lumbermill;


import lumbermill.api.JsonEvent;
import lumbermill.internal.influxdb.InfluxDBClient;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;

/**
 * Handle to InfluxDB
 */
public class Influxdb {

  public static final Influxdb influxdb = new Influxdb ();

  private InfluxDBClient client = new InfluxDBClient ();

  public Func1<List<JsonEvent>, Observable<List<JsonEvent>>> client(Map map) {
    return client.client (map);
  }
}
