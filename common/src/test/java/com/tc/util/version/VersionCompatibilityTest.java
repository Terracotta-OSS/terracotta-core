/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import junit.framework.TestCase;

public class VersionCompatibilityTest extends TestCase {

  private VersionCompatibility versionCompatibility;

  @Override
  protected void setUp() throws Exception {
    this.versionCompatibility = new VersionCompatibility();
  }

  public void testNull() {
    try {
      versionCompatibility.isCompatibleClientServer(v("1.0.0"), null);
      fail();
    } catch (NullPointerException npe) {
      // expected
    }

    try {
      versionCompatibility.isCompatibleClientServer(null, v("1.0.0"));
      fail();
    } catch (NullPointerException npe) {
      // expected
    }

    try {
      versionCompatibility.isCompatibleClientServer(null, null);
      fail();
    } catch (NullPointerException npe) {
      // expected
    }
  }

  public void testSame() {
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0"), v("1.0.0")));
  }

  public void testMajorBump() {
    assertFalse(versionCompatibility.isCompatibleClientServer(v("1.0.0"), v("2.0.0")));
  }

  public void testMajorDrop() {
    assertFalse(versionCompatibility.isCompatibleClientServer(v("2.0.0"), v("1.0.0")));
  }

  public void testMinorBump() {
    assertFalse(versionCompatibility.isCompatibleClientServer(v("1.0.0"), v("1.1.0")));
  }

  public void testMinorDrop() {
    assertFalse(versionCompatibility.isCompatibleClientServer(v("1.1.0"), v("1.0.0")));
  }

  public void testDotBump() {
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0"), v("1.0.1")));
  }

  public void testDotDrop() {
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.1"), v("1.0.1")));
  }

  public void testSnapshots() {
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0"), v("1.0.0-SNAPSHOT")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0-SNAPSHOT"), v("1.0.0")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0-SNAPSHOT"), v("1.0.0-SNAPSHOT")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0"), v("1.0.1-SNAPSHOT")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0-SNAPSHOT"), v("1.0.1")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.0-SNAPSHOT"), v("1.0.1-SNAPSHOT")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.1"), v("1.0.0-SNAPSHOT")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.1-SNAPSHOT"), v("1.0.0")));
    assertTrue(versionCompatibility.isCompatibleClientServer(v("1.0.1-SNAPSHOT"), v("1.0.0-SNAPSHOT")));

    assertFalse(versionCompatibility.isCompatibleClientServer(v("1.1.0"), v("1.0.0-SNAPSHOT")));
    assertFalse(versionCompatibility.isCompatibleClientServer(v("1.1.0-SNAPSHOT"), v("1.0.0")));
    assertFalse(versionCompatibility.isCompatibleClientServer(v("1.1.0-SNAPSHOT"), v("1.0.0-SNAPSHOT")));

    assertFalse(versionCompatibility.isCompatibleClientServer(v("2.0.0"), v("1.0.0-SNAPSHOT")));
    assertFalse(versionCompatibility.isCompatibleClientServer(v("2.0.0-SNAPSHOT"), v("1.0.0")));
    assertFalse(versionCompatibility.isCompatibleClientServer(v("2.0.0-SNAPSHOT"), v("1.0.0-SNAPSHOT")));
  }

  private static Version v(String version) {
    return new Version(version);
  }

}
