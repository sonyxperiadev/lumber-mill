package lumbermill.api;

import lumbermill.internal.StringTemplate;

import java.util.Optional;

public class Sys {

  public static Env env (String name, String defaulT) {
    return System.getenv (name) != null ? new Env(System.getenv (name)) : new Env(defaulT);
  }

  public static Env env (String name) {
    return new Env(name);
  }

  public static class Env {

    private final StringTemplate value;

    Env (String value) {
      this.value = StringTemplate.compile(value);
    }

    public String string() {
      Optional<String> stringOptional = value.format();
      if (stringOptional.isPresent()) {
        return stringOptional.get();
      }
      throw new IllegalStateException("No environment variable found for " + value.original());
    }

    public boolean bool() {
      return Boolean.valueOf (string());
    }

    public int number() {
      return Integer.parseInt (string());
    }

  }
}
