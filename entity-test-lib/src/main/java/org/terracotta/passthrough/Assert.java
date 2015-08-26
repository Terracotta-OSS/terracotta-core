package org.terracotta.passthrough;

public class Assert {

  public static void fail(Exception e) {
    throw new AssertionError("Always fail", e);
  }

  public static void unimplemented() {
    throw new AssertionError("Case not implemented");
  }

  public static void unreachable() {
    throw new AssertionError("Could path should not be reachable");
  }

  public static void assertTrue(boolean test) {
    if (!test) {
      throw new AssertionError("Case must be true");
    }
  }
}
