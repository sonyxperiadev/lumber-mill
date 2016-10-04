package lumbermill.net.api;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import lumbermill.api.BytesEvent;
import lumbermill.api.Event;
import lumbermill.internal.net.VertxHttpRequestWrapper;
import lumbermill.internal.net.VertxReconnectableSocket;
import lumbermill.internal.net.VertxTCPServer;
import okio.ByteString;
import rx.Observable;

import java.net.URI;
import java.util.function.Consumer;

import static java.lang.String.format;


/**
 * Utility methods for tcp and http.
 */
public class Net {

  private static final Vertx vertx = Vertx.vertx ();

  public interface TCPServer {
    TCPServer listen(Consumer<BytesEvent> consumer);
    void close();
  }

  public interface HttpRequest {
    HttpRequest okOn (Integer... statuses);
    Observable<HttpResponse> write (ByteString data);
    Observable<HttpResponse> write (Event data);
  }

  public interface HttpResponse {
    BytesEvent data();
    int status();
  }


  static Socket socket (String host, int port) {
    return new VertxReconnectableSocket (URI.create (format("tcp://%s:%s", host, port)));
  }

  static Socket socket (URI uri) {
    return new VertxReconnectableSocket (uri);
  }


  public static HttpRequest rxify (HttpClientRequest client) {
    return new VertxHttpRequestWrapper (client);
  }

  public static HttpClient httpClient(HttpClientOptions options) {
    return vertx.createHttpClient (options);
  }

  public static VertxTCPServer tcpServer(String bindAddress, int port) {
    return new VertxTCPServer (bindAddress, port);
  }
}
