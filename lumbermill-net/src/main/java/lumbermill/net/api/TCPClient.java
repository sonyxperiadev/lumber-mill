package lumbermill.net.api;


import okio.ByteString;


public interface TCPClient {

  boolean isConnected ();
  void close ();
  TCPClient write (ByteString... data);
}
