package lumbermill;


import lumbermill.api.Codecs;
import lumbermill.api.Event;
import lumbermill.internal.SimpleGraphiteClient;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class GraphiteEventsTest<T extends Event> extends AbstractHttpServerTest {

  public static final String TIME_ISO_8601 = "2016-09-28T09:39:57.222+02:00";
  public static final long TIME_MILLIS = 1475048397300L;
  public static final int TIME_SECONDS = 1475048397;


  public static final String TAG = "thetag";
  String contentType = "text/plain";
  String path = "/events";

  Http.Server<T> server = null;

  @Before
  public void prepareHttp() {
    server = withTags(TAG).prepare(path);
  }


  @Test
  public void test_tag_is_correct_with_each() {

    SimpleGraphiteClient client = SimpleGraphiteClient.create ()
      .withEventServer ("localhost", 9876);

    server.on(observable -> observable
      .filter(t -> t.hasTag(TAG))
      .doOnNext(subscriber().action1()));

      client.write (Codecs.TEXT_TO_JSON.from ("hello"))
        .doOnNext (Core.console.stdout ())
        .subscribe ();

 //   post(path, "Hello", contentType, 200);

    await().atMost(1, TimeUnit.SECONDS).until(subscriber().onNextInvoked(1));
    Assertions.assertThat(subscriber.lastEvent().hasTag(TAG)).isTrue();
  }



}
