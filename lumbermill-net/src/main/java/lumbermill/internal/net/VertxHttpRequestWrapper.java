package lumbermill.internal.net;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import lumbermill.api.BytesEvent;
import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.net.api.Net;
import okio.ByteString;
import rx.Observable;
import rx.subjects.ReplaySubject;

import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;


public class VertxHttpRequestWrapper implements Net.HttpRequest {

  private final HttpClientRequest request;
  private List<Integer> okStatuses = Collections.emptyList ();

  public VertxHttpRequestWrapper (HttpClientRequest client) {
    this.request = client;
  }

  public Observable<Net.HttpResponse> write (ByteString data) {

    Buffer buffer = Buffer.buffer (data.toByteArray ());
      ReplaySubject<Net.HttpResponse> subject = ReplaySubject.createWithSize(1);

      request.handler (response ->  {

          if (!okStatuses.contains (response.statusCode ())) {
            throw new IllegalStateException (format("Unexpected statusCode %s and message %S",
              response.statusCode (), response.statusMessage ()));
          }

          response.bodyHandler (body -> {

            subject.onNext (
              new Net.HttpResponse () {

                @Override
                public BytesEvent data () {
                  return Codecs.BYTES.from (body.getBytes ());
                }

                @Override
                public int status () {
                  return response.statusCode ();
                }
            });
            subject.onCompleted ();
          });
        })
        .exceptionHandler ( ex -> subject.onError (ex))
        .putHeader ("Content-Length", String.valueOf (buffer.length ()))
        .end (buffer);

      return subject;
    }

  @Override
  public Observable<Net.HttpResponse> write (Event data) {
    return write (data.raw ());
  }

  @Override
  public Net.HttpRequest okOn (Integer... statused) {
    this.okStatuses = asList(statused);
    return this;
  }


}
