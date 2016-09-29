package lumbermill.internal.net;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import lumbermill.api.BytesEvent;
import lumbermill.api.Codecs;
import okio.ByteString;

import java.util.function.Consumer;


/**
 * Experimental Dummy TCP Server currently used in tests only
 *
 */
public class VertxTCPServer {

  private static final Vertx vertx = Vertx.vertx ();

  private final String bindAddress;
  private final int port;
  NetServer server;
  boolean dummyConnected = false;

  public VertxTCPServer (String bindAddress, int port) {
    this.bindAddress = bindAddress;
    this.port = port;
  }

  public void close() {
    server.close ();
    server = null;
  }

  public boolean isStarted () {
    return dummyConnected;
  }

  public VertxTCPServer listen(Consumer<BytesEvent> consumer) {
    server = vertx.createNetServer();
    server.connectHandler(socket -> {
      socket.handler(buffer -> {
        ByteString data = ByteString.of (buffer.getBytes ());
        consumer.accept (Codecs.BYTES.from (data));
      });
      socket.closeHandler (handler -> dummyConnected = false);

    });
    server.listen(port, bindAddress, res -> {
      if (res.succeeded()) {
        System.out.println("Server is now listening!");
        dummyConnected = true;
      } else {
        System.out.println("Failed to bind!");
        dummyConnected = false;
      }
    });
    return this;
  }
}


