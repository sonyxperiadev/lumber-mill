package lumbermill.influxdb;


import lumbermill.api.JsonEvent;
import lumbermill.influxdb.internal.InfluxDBClient;
import rx.Observable;
import rx.functions.Func1;

import java.util.List;
import java.util.Map;


public class Influxdb {

  private static final Influxdb influxdb = new Influxdb ();

  private InfluxDBClient client = new InfluxDBClient ();


  public Func1<List<JsonEvent>, Observable<List<JsonEvent>>> client(Map map) {
    return client.client (map);
  }
}
