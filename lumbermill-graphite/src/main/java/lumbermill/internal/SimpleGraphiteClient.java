package lumbermill.internal;


import lumbermill.internal.net.VertxTCPClient;
import lumbermill.net.api.TCPClient;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Map<URI, TCPClient> clients = new HashMap<> ();

  private final TCPClient tcpClient;

  public SimpleGraphiteClient (String host, int port) {
    URI uri = URI.create (format ("tcp://%s:%s", host, port));
    if (clients.containsKey (uri)) {
      LOGGER.info ("Found existing tcp client for {}", uri);
      this.tcpClient = clients.get (uri);
    } else {
      LOGGER.info ("Creating new tcp client for {}", uri);
      this.tcpClient = new VertxTCPClient (uri).connect ();
      clients.put (uri, this.tcpClient);
    }
  }

  public SimpleGraphiteClient (TCPClient tcpClient) {
    this.tcpClient = tcpClient;
  }

  public void save (Metric metric) {

    if (LOGGER.isTraceEnabled ()) {
      LOGGER.trace (metric.format ().utf8 ());
    }
    tcpClient.write (metric.format ());
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

      private ByteString format() {
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
