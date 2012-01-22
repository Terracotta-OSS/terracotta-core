/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import java.util.Properties;

import junit.framework.TestCase;

public class NIOWorkaroundsTest extends TestCase {

  public void testSolaris10Workaround() {
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_09")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_08")));
    assertTrue(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0_07")));
    assertTrue(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.5.0")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.6.0")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.7.0")));
    assertTrue(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.10", "1.4.2_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "SunOS", "5.9", "1.4.2_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "Linux", "5.10", "1.4.2_11")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("sun", "Linux", "5.10", "1.5.0_09")));
    assertFalse(NIOWorkarounds.solaris10Workaround(makeProps("bea", "SunOS", "5.10", "1.5.0_09")));
  }

  private static Properties makeProps(String vendor, String osName, String osVersion, String javaVersion) {
    Properties props = new Properties();
    props.setProperty("java.vendor", vendor);
    props.setProperty("os.name", osName);
    props.setProperty("os.version", osVersion);
    props.setProperty("java.version", javaVersion);
    return props;
  }

}
