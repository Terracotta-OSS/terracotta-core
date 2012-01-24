/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles;

import junit.framework.TestCase;

public class MavenToOSGiTest extends TestCase {

  public void testSymbolicName() {
    // Nulls
    try { 
      helpTestSymbolicName(null, null, null);
      fail("Expected error on no groupId or artifactId");
    } catch(IllegalArgumentException e) {
      // expected error, testing invalid condition
    }
    helpTestSymbolicName(null, "a", "a");
    helpTestSymbolicName("a", null, "a");
    
    // Empty strings
    try { 
      helpTestSymbolicName("", "", null);
      fail("Expected error on no groupId or artifactId");
    } catch(IllegalArgumentException e) {
      // expected error, testing invalid condition
    }
    helpTestSymbolicName("", "a", "a");
    helpTestSymbolicName("a", "", "a");

    // Normal
    helpTestSymbolicName("org.foo", "bar", "org.foo.bar");

    // Dashes
    helpTestSymbolicName("org.foo", "bar-with-dash", "org.foo.bar-with-dash");
    helpTestSymbolicName("org-foo", "bar-baz", "org-foo.bar-baz");

    // Invalid chars
    helpTestSymbolicName("a #4-a_0.b-c.-d", "-#123!@#$%^&*() \t", "a__4-a_0.b-c.-d.-_123____________");
  }

  private static void helpTestSymbolicName(String groupId, String artifactId, String expectedSymbolicName) {
    String sName = MavenToOSGi.artifactIdToSymbolicName(groupId, artifactId);
    assertEquals(expectedSymbolicName, sName);
  }
  
  public void testVersion() {
    helpTestVersion(1, 2, 3, null, "1.2.3");
    helpTestVersion(1, 0, 0, "SNAPSHOT", "1.0.0.SNAPSHOT");
    helpTestVersion(1, 0, 0, "RC-1", "1.0.0.RC-1");
    helpTestVersion(1, 2, 3, "A B:C-D@E#f$g%", "1.2.3.A_B_C-D_E_f_g_");
  }
  
  private static void helpTestVersion(int majorVersion, int minorVersion, int incrementalVersion, String qualifier, String expected) {
    String sVersion = MavenToOSGi.projectVersionToBundleVersion(majorVersion, minorVersion, incrementalVersion, qualifier);
    assertEquals(expected, sVersion);
  }

  public void testVersionString() {
    helpTestVersionString("1", "1.0.0");
    helpTestVersionString("1.2", "1.2.0");
    helpTestVersionString("1.2.3", "1.2.3");
    helpTestVersionString("1.2.3-SNAPSHOT", "1.2.3.SNAPSHOT");
    helpTestVersionString("1.2.3-RC-2", "1.2.3.RC-2");
    helpTestVersionString("1.2-SNAPSHOT", "1.2.0.SNAPSHOT");
    helpTestVersionString("1-SNAPSHOT", "1.0.0.SNAPSHOT");
    helpTestVersionString("RELEASE", "0.0.0.RELEASE");
    helpTestVersionString("", "0.0.0");
    helpTestVersionString(null, "0.0.0");
    helpTestVersionString("1.2.3-A B:C-D@E#f$g%", "1.2.3.A_B_C-D_E_f_g_");
    
    // test getting passed an already valid OSGI version 
    helpTestVersionString("1.2.3.SNAPSHOT", "1.2.3.SNAPSHOT");    
  }

  private static void helpTestVersionString(String mavenVersion, String expected) {
    String sVersion = MavenToOSGi.projectVersionToBundleVersion(mavenVersion);
    assertEquals(expected, sVersion);
  }


}
