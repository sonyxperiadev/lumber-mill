package lumbermill.api;


import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonEventTest {

  @Test
  public void test_replace_value() {
      Codecs.TEXT_TO_JSON.from ("hello")
      .put ("value", 1)
      .rename ("value", "result")
      .<JsonEvent>toObservable ()
      .doOnNext (event -> assertThat(event.has ("value")).isFalse ())
        .doOnNext (event -> assertThat(event.has ("result")).isTrue ())
        .doOnNext (event -> assertThat(event.asDouble ("result")).isEqualTo (1))
        .subscribe ();
  }

  @Test
  public void test_replace_value_does_not_change_when_non_existing() {
    Codecs.TEXT_TO_JSON.from ("hello")
      .put ("value", 1)
      .rename ("value_", "result")
      .<JsonEvent>toObservable ()
      .doOnNext (event -> assertThat(event.has ("value")).isTrue ())
      .doOnNext (event -> assertThat(event.has ("result")).isFalse ())
      .doOnNext (event -> assertThat(event.asDouble ("value")).isEqualTo (1))
      .subscribe ();
  }

}
