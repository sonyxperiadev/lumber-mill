package lumbermill.net.api;


import okio.ByteString;


public interface Socket {

  boolean isConnected ();
  void close ();
  Socket write (ByteString... data);
}
