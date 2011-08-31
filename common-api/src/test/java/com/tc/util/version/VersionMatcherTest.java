/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import com.tc.util.version.VersionMatcher;

import junit.framework.TestCase;

public class VersionMatcherTest extends TestCase {

  public void testExactMatch() {
    VersionMatcher matcher = new VersionMatcher("3.0.0", "1.0.0");
    assertTrue(matcher.matches("3.0.0", "1.0.0"));
  }
  
  public void testExactMisMatch() { 
    VersionMatcher matcher = new VersionMatcher("3.0.0", "1.0.0");
    assertFalse(matcher.matches("3.0.0", "9.9.9"));
    assertFalse(matcher.matches("9.9.9", "1.0.0"));
  }
  
  public void testTcAny() {
    VersionMatcher matcher = new VersionMatcher("3.0.0", "1.0.0");
    assertTrue(matcher.matches("*", "1.0.0"));
    assertFalse(matcher.matches("*", "2.0.0"));
    assertTrue(matcher.matches("*", "[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)"));
  }
  
  /**
   * For given module specified by range, verify different release versions
   * match against it properly.
   */
  public void testInRangeWithSnapshot() {
    VersionMatcher matcher = new VersionMatcher("3.0.0", "1.0.0-SNAPSHOT");
    assertTrue(matcher.matches("*", "[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)"));

    VersionMatcher matcher1 = new VersionMatcher("3.0.0", "1.0.0");
    assertTrue(matcher1.matches("*", "[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)"));

    VersionMatcher matcher2 = new VersionMatcher("3.0.0", "1.0.1");
    assertTrue(matcher2.matches("*", "[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)"));  

    VersionMatcher matcher3 = new VersionMatcher("3.0.0", "1.1.0-SNAPSHOT");
    assertFalse(matcher3.matches("*", "[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)"));  

    VersionMatcher matcher4 = new VersionMatcher("3.0.0", "1.1.0");
    assertFalse(matcher4.matches("*", "[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)"));  

  }


}
