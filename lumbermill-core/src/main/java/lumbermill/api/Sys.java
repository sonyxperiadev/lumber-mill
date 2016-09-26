package lumbermill.api;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class Sys {

  public static Env env (String name, String defaulT) {
    return System.getenv (name) != null ? new Env(System.getenv (name)) : new Env(defaulT);
  }

  public static Env env (String name) {
    String value =  System.getenv (name) != null ? System.getenv (name) : null;
    if (value != null) {
      return new Env(value);
    }
    throw new IllegalStateException (format("No System env found for name %s", name));
  }

  public static class Env {

    private final String value;

    Env (String value) {
      this.value = value;
    }

    public String string() {
      return value;
    }

    public boolean bool() {
      return Boolean.valueOf (value);
    }

    public int number() {
      return Integer.parseInt (value);
    }

  }
}
