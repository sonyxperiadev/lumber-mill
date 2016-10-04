package lumbermill.internal;

import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import lumbermill.api.BytesEvent;
import lumbermill.api.JsonEvent;
import lumbermill.internal.net.VertxReconnectableSocket;
import lumbermill.net.api.Net;
import lumbermill.net.api.Socket;
import okio.ByteString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public class SimpleGraphiteClient {

  private static final Logger LOGGER = LoggerFactory.getLogger (SimpleGraphiteClient.class);

  /**
   * Keeps a global map with host:port pairs to prevent from creating multipe clients
   */
  private static final Map<URI, Socket> clients = new HashMap<> ();

  private Socket socket;

  private HttpClient httpClient;

  public SimpleGraphiteClient () {

  }


  public static SimpleGraphiteClient create() {
    return new SimpleGraphiteClient ();
  }


  public SimpleGraphiteClient carbonServer(Socket socket) {
    this.socket = socket;
    return this;
  }

  public SimpleGraphiteClient carbonServer(String host, int tcpPort) {
    URI uri = URI.create (format ("tcp://%s:%s", host, tcpPort));
    if (clients.containsKey (uri)) {
      LOGGER.info ("Found existing tcp client for {}", uri);
      this.socket = clients.get (uri);
    } else {
      LOGGER.info ("Creating new tcp client for {}", uri);
      this.socket = new VertxReconnectableSocket (uri).connect ();
      clients.put (uri, this.socket);
    }
    return this;
  }

  public SimpleGraphiteClient withEventServer (String host, int port) {
      httpClient = Net.httpClient (new HttpClientOptions ().setDefaultHost (host).setDefaultPort (port));
     return this;
  }




  public void save (Metric metric) {

    if (LOGGER.isTraceEnabled ()) {
      LOGGER.trace (metric.format ().utf8 ());
    }
    socket.write (metric.format ());
  }

  public Observable<BytesEvent> write(JsonEvent event) {

    return Net.rxify (httpClient.post ("/events/")
      .putHeader ("Content-Type", "application/json"))
      .okOn(200)
      .write (event)
      .map (httpResponse -> httpResponse.data ());
  }


  private static HttpClientOptions withProxyOptions (HttpClientOptions options) {
    String https_proxy = System.getenv ("https_proxy");
    if (StringUtils.isNotEmpty (https_proxy)) {
      URI proxy = URI.create (https_proxy);
      options.setProxyOptions (new ProxyOptions ().setHost (proxy.getHost ()).setPort (proxy.getPort ()));
    }
    return options;
  }

  public static class Metric {

      private String metric;
      private double value;
      private long timeInSeconds;

      private Metric(String metric, double value) {
        this.metric = metric;
        this.value = value;
      }

      public static Metric create(String metric, double value) {
       return new Metric (metric, value);
      }

      public Metric withIsoTime(String isoTime) {
          timeInSeconds =  ZonedDateTime.parse(isoTime,
          DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() / 1000;
          return this;
      }

      public ByteString format() {
        return ByteString.of (String.format("%s %s %d%n", metric, value, timeInSeconds).getBytes ());
      }

    public Metric withTimeInSecs (long l) {
      this.timeInSeconds = l;
      return this;
    }

    public Metric withTimeInMillis (long l) {
        this.timeInSeconds = l / 1000;
        return this;
    }
  }
}
