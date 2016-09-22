package lumbermill.api;

public class Helpers {

  public static String sysenv(String name, String defaulT) {
    return System.getenv (name) != null ? System.getenv (name) : defaulT;
  }
}
