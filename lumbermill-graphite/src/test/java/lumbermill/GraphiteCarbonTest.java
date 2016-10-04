package lumbermill;


import lumbermill.api.BytesEvent;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.api.JsonEvent;
import lumbermill.internal.MapWrap;
import lumbermill.internal.net.VertxTCPServer;
import org.assertj.core.api.Assertions;
import org.awaitility.Duration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Java6Assertions.assertThat;

@Ignore
public class GraphiteCarbonTest {

  public static final String TIME_ISO_8601 = "2016-09-28T09:39:57.222+02:00";
  public static final long TIME_MILLIS = 1475048397300L;
  public static final int TIME_SECONDS = 1475048397;

  private static VertxTCPServer graphiteServer = new VertxTCPServer ("localhost", 5432);

  private static Queue<BytesEvent> receivedMetrics = new ConcurrentLinkedQueue<> ();


  @BeforeClass
  public static void prepareTcpServer () {
    graphiteServer.listen (event -> receivedMetrics.add (event));
    await ().atMost (1, TimeUnit.SECONDS).until (() -> graphiteServer.isStarted ());
  }


  @After
  @Before
  public void clear () {
    receivedMetrics.clear ();
  }


  @AfterClass
  public static void shutdownTcpServer () {
    graphiteServer.close ();
    await ().atMost (1, TimeUnit.SECONDS).until (() -> !graphiteServer.isStarted ());
  }


  @Test
  public void test_metrics_parsed_correctly_using_iso_date () {
    json ()
      .put ("@timestamp", TIME_ISO_8601) // Overwrite generated timestamp
      .<JsonEvent>toObservable ()
      .flatMap (
        graphite ("@timestamp", "ISO_8601"))
      .toBlocking ().subscribe ();

    verify ();
  }

  @Test
  public void test_metrics_parsed_correctly_with_prefix () {
    json ()
      .put ("@timestamp", TIME_ISO_8601) // Overwrite generated timestamp
      .<JsonEvent>toObservable ()
      .flatMap (
        graphite ("@timestamp", "ISO_8601","prefix."))
      .toBlocking ().subscribe ();

    await ().atMost (2, TimeUnit.SECONDS).until (() -> receivedMetrics.size () == 2);
    assertThat (receivedMetrics.poll ().raw ().utf8 ().trim ()).isEqualTo ("prefix.stats.counters.hits.count 5.0 1475048397");
    assertThat (receivedMetrics.poll ().raw ().utf8 ().trim ()).isEqualTo ("prefix.stats.counters.2.hits.count 5.0 1475048397");
  }


  @Test
  public void test_metrics_parsed_correctly_using_millis () {
    json ()
      .put ("time", TIME_MILLIS)
      .<JsonEvent>toObservable ()
      .flatMap (
        graphite ("time", "MILLIS"))
      .toBlocking ().subscribe ();

    verify ();
  }

  //@Test
  public void test_metrics_parsed_correctly_using_seconds () {

    json ()
      .put ("time", TIME_SECONDS)
      .<JsonEvent>toObservable ()
      .flatMap (
        graphite ("time", "SECONDS"))
      .toBlocking ().subscribe ();

    verify ();
  }

  @Test
  public void test_metric_field_does_not_exist_is_not_stored() {
    json ()
      .remove ("@metric") // Remove field that is used to create metric
      .<JsonEvent>toObservable ()
      .flatMap (graphite ("@timestamp", "ISO_8601"))
      .toBlocking ().subscribe ();

    await ().timeout (Duration.ONE_SECOND).until (() -> receivedMetrics.size () == 0);
  }

  @Test
  public void test_value_field_does_not_exist_is_not_stored() throws InterruptedException {
    json ()
      .remove ("@value") // Remove field that is used to create metric
      .<JsonEvent>toObservable ()
      .flatMap (graphite ("@timestamp", "ISO_8601"))
      .toBlocking ().subscribe ();

    Thread.sleep (1000);
    assertThat (receivedMetrics.size ()).isEqualTo (0);
  }


  public Func1<JsonEvent, Observable<JsonEvent>> graphite(String field, String precision) {
    return graphite(field, precision,"");
  }

  public Func1<JsonEvent, Observable<JsonEvent>> graphite(String field, String precision, String prefix) {
    return Graphite.carbon (MapWrap.of (
      "port", 5432,
      "timestamp_precision", precision,
      "timestamp_field", field,
      "prefix", prefix,
      "metrics", MapWrap.of (
        "stats.counters.{@metric}", "{@value}",
        "stats.counters.2.{@metric}", "{@value}").toMap ()
    ).toMap ());
  }





  public JsonEvent json() {
    return Codecs.TEXT_TO_JSON.from ("hello")
      .put ("@metric", "hits.count")
      .put ("@value", 5);
  }

  public void verify() {
    await ().atMost (2, TimeUnit.SECONDS).until (() -> receivedMetrics.size () == 2);
    assertThat (receivedMetrics.poll ().raw ().utf8 ().trim ()).isEqualTo ("stats.counters.hits.count 5.0 1475048397");
    assertThat (receivedMetrics.poll ().raw ().utf8 ().trim ()).isEqualTo ("stats.counters.2.hits.count 5.0 1475048397");
  }

}
