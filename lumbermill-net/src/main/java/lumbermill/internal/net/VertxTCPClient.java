package lumbermill.internal.net;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import lumbermill.net.api.TCPClient;
import okio.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;


public class VertxTCPClient implements TCPClient {

  private static final Logger LOGGER = LoggerFactory.getLogger (VertxTCPClient.class);

  private final URI uri;
  private final Vertx vertx = Vertx.vertx ();
  private final NetClient client;
  private final AtomicReference<NetSocket> socket = new AtomicReference<> ();
  private final Semaphore connectLock = new Semaphore (1);
  private final AtomicBoolean closeRequested = new AtomicBoolean (false);

  public VertxTCPClient (URI uri) {
   this(uri, new NetClientOptions()
     .setConnectTimeout(1000)
     .setReconnectAttempts (5)
     .setReconnectInterval (1000));
  }

  public VertxTCPClient (URI uri, NetClientOptions options) {
    client = vertx.createNetClient(options);
    this.uri = uri;
  }


  public VertxTCPClient write(ByteString... data) {
      LOGGER.debug ("write():length:{}", data.length);
      NetSocket socket = this.socket.get ();
      for (ByteString s : data) {
        if (!isConnected ()) {
          LOGGER.error ("Not connected to server, unable to send data to {}", uri);
          throw new IllegalStateException (format("Not connected, unable to send data to %s", uri));
        }
        System.out.println ("full? " + socket.writeQueueFull ());
        socket.write (Buffer.buffer (s.toByteArray ()));
      }
    return this;
  }

  public void close() {
    if (isConnected ()) {
      closeRequested.set (true);
      socket.get ().close ();
    }
  }



  public boolean isConnected () {
    return socket.get () != null;
  }

  public VertxTCPClient write(List<ByteString> data) {
    return write(data.toArray (new ByteString[0]));
  }

  public VertxTCPClient connect() {
    if (!connectLock.tryAcquire ()) {
      LOGGER.info ("Connect attempt already in progress");
      return this;
    }

    client.connect(uri.getPort (), uri.getHost (), res -> {
      if (res.succeeded()) {
        connectLock.release ();
        LOGGER.info("Connected to {}", uri);
        NetSocket socket = res.result();
        socket.closeHandler (reconnectOnClose ());
        socket.handler (responseHandler ());
        socket.exceptionHandler (exceptionHandler());
        this.socket.set (socket);
      } else {
        connectLock.release ();
        LOGGER.warn ("Failed to connect: " + res.cause().getMessage());
          reconnect ();
      }
    });
    return this;
  }

  private VertxTCPClient reconnect() {
    socket.set(null);
    return connect ();
  }

  private Handler<Throwable> exceptionHandler () {
    return throwable -> {
     LOGGER.warn ("Received exception {}:{}", throwable.getClass ().getSimpleName (), throwable.getMessage ());
      reconnect ();
    };
  }

  private Handler<Void> reconnectOnClose () {
    return event -> {
      if (!closeRequested.get ()) {
        LOGGER.warn ("Socket closed, attempting to reconnect");
        reconnect ();
      }
    };
  }

  private Handler<Buffer> responseHandler () {
    return buffer -> {
      if (LOGGER.isTraceEnabled ()) {
        LOGGER.trace ("TCP Response: " + ByteString.of (buffer.getBytes ()).utf8 ());
      }
    };
  }
}
