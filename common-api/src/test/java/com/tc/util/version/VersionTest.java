/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.version;

import com.tc.util.version.Version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

  private void helpTestParse(String str, int major, int minor, int micro, String qualifier) {
    Version v = new Version(str);
    assertEquals("major", major, v.major());
    assertEquals("minor", minor, v.minor());
    assertEquals("micro", micro, v.micro());
    assertEquals("qualifier", qualifier, v.qualifier());
  }
  
  public void testVersionParse() {
    helpTestParse("1", 1, 0, 0, null);
    helpTestParse("1.2", 1, 2, 0, null);
    helpTestParse("1.2.3", 1, 2, 3, null);
    helpTestParse("1.2.3-foo", 1, 2, 3, "foo");
  }
  
  private void helpTestInvalid(String input) {
    try {
      new Version(input);
      fail("Expected error on invalid input but got none: " + input);
    } catch(IllegalArgumentException e) {
      // expected
    }
  }
  
  public void testVersionInvalid() {
    helpTestInvalid("foo");
    helpTestInvalid("1.1.1.1");
    helpTestInvalid("1.1.1.SNAPSHOT");
    helpTestInvalid("[1.0.0,1.1.0)");
  }
  
  private void helpTestCompare(String s1, String s2, int compareDirection) {
    Version v1 = new Version(s1);
    Version v2 = new Version(s2);
    int compared = v1.compareTo(v2);
    int comparedBackwards = v2.compareTo(v1);
    
    if(compareDirection == 0) {
      assertTrue("expected 0, got: " + compared, compared == 0);
      assertTrue(comparedBackwards == 0);
    } else if(compareDirection < 0) {
      assertTrue(compared < 0);
      assertTrue(comparedBackwards > 0);
    } else {
      assertTrue(compared > 0);
      assertTrue(comparedBackwards < 0);
    }
  }
  
  public void testComparison() {
    helpTestCompare("1.0.0", "1.0.0", 0);
    helpTestCompare("1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT", 0);
    helpTestCompare("1.0.0", "1.0.0-SNAPSHOT", 1);
    helpTestCompare("1.0.0", "1.1.0", -1);
    helpTestCompare("1.0.1", "1.0.2", -1);
    helpTestCompare("1.0.0", "2.0.0", -1);
    helpTestCompare("1.2.3", "4.5.6", -1);
  }
  
  public void testSortList() {
    List stuff = new ArrayList();
    stuff.add(new Version("1.2.0")); 
    stuff.add(new Version("1.1.0-SNAPSHOT"));
    stuff.add(new Version("1.1.0"));
    stuff.add(new Version("1.0.0"));
    stuff.add(new Version("2.1.0"));
    stuff.add(new Version("2.1.0-SNAPSHOT"));
    Collections.sort(stuff);
    assertEquals("[1.0.0, 1.1.0.SNAPSHOT, 1.1.0, 1.2.0, 2.1.0.SNAPSHOT, 2.1.0]", stuff.toString());
  }
}
