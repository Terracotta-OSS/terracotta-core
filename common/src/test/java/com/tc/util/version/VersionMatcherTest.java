/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import junit.framework.TestCase;

public class VersionMatcherTest extends TestCase {

  public void testExactMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    assertTrue(matcher.matches("3.0.0"));
  }

  public void testExactMisMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    assertFalse(matcher.matches("9.9.9"));
  }

  public void testTcAny() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    assertTrue(matcher.matches("*"));
  }

}
