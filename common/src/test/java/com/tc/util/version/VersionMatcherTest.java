/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.SmokeTests;

@Category(SmokeTests.class)
public class VersionMatcherTest {

  public void testExactMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    Assert.assertTrue(matcher.matches("3.0.0"));
  }

  public void testExactMisMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    Assert.assertFalse(matcher.matches("9.9.9"));
  }

  public void testTcAny() {
    VersionMatcher matcher = new VersionMatcher("3.0.0");
    Assert.assertTrue(matcher.matches("*"));
  }

}
