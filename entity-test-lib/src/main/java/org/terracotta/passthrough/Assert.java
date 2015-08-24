package org.terracotta.passthrough;


/**
 * Common assertion utilities used by the pass-through system-test.
 */
public class Assert {

  public static void unexpected(Exception e) {
    throw (AssertionError) new AssertionError("Unexpected exception").initCause(e);
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
