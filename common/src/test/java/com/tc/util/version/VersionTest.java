/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.util.version;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

  private void helpTestParse(String str, int major, int minor, int micro, int patch, int build, String specifier, String qualifier) {
    Version v = new Version(str);
    assertEquals("major", major, v.major());
    assertEquals("minor", minor, v.minor());
    assertEquals("micro", micro, v.micro());
    assertEquals("specifier", specifier, v.specifier());
    assertEquals("qualifier", qualifier, v.qualifier());
  }
  
  public void testVersionParse() {
    helpTestParse("1", 1, 0, 0, 0, 0, null, null);
    helpTestParse("1.2", 1, 2, 0, 0, 0, null, null);
    helpTestParse("1.2.3", 1, 2, 3, 0, 0, null, null);
    helpTestParse("1.2.3-foo", 1, 2, 3, 0, 0, null, "foo");
    helpTestParse("1.2.3_preview-foo", 1, 2, 3, 0, 0, "preview", "foo");
    helpTestParse("1.2.3_preview", 1, 2, 3, 0, 0, "preview", null);
    helpTestParse("4.3.0.0.1", 4, 3, 0, 0, 1, null, null);
  }
  
  @SuppressWarnings("unused")
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
    helpTestInvalid("1.1.1.SNAPSHOT");
    helpTestInvalid("[1.0.0,1.1.0)");
    helpTestInvalid("1.0.thisdoesntlookright");
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
    helpTestCompare("1.0_preview", "1.0", -1);
    helpTestCompare("1.0-SNAPSHOT", "1.0_preview", 1);
    helpTestCompare("1.0_preview-SNAPSHOT", "1.0_preview", -1);
    helpTestCompare("1.0_preview1", "1.0_preview2", -1);
    helpTestCompare("1.0_preview1-SNAPSHOT", "1.0_preview2", -1);
    helpTestCompare("1.0_preview2-SNAPSHOT", "1.0_preview1", 1);
    helpTestCompare("4.3.1.0.1", "4.3.1.0.2", -1);
    helpTestCompare("4.3.0", "4.3.0.0.1", -1);
    helpTestCompare("4.3.0.0.256", "4.4.0", -1);
  }
  
  public void testSortList() {
    List<Version> stuff = new ArrayList<Version>();
    stuff.add(new Version("1.2.0")); 
    stuff.add(new Version("1.1.0-SNAPSHOT"));
    stuff.add(new Version("1.1.0"));
    stuff.add(new Version("1.0.0"));
    stuff.add(new Version("2.1.0"));
    stuff.add(new Version("2.1.0-SNAPSHOT"));
    stuff.add(new Version("1.1.0_preview"));
    Collections.sort(stuff);
    assertEquals("[1.0.0.0.0, 1.1.0.0.0_preview, 1.1.0.0.0-SNAPSHOT, 1.1.0.0.0, 1.2.0.0.0, 2.1.0.0.0-SNAPSHOT, 2.1.0.0.0]", stuff.toString());
  }

  public void testIsNewer() {
    try {
      helpTestIsNewer ("1", "1", 0);
      fail("depth must be >= 1 and <= 5");
    } catch(IndexOutOfBoundsException e) {
      // expected
    }
    try {
      helpTestIsNewer ("1", "1", 6);
      fail("depth must be >= 1 and <= 5");
    } catch(IndexOutOfBoundsException e) {
      // expected
    }

    assertTrue(helpTestIsNewer("11", "10", 1));
    assertTrue(helpTestIsNewer("11", "10", 2));
    assertTrue(helpTestIsNewer("11", "10", 3));
    assertTrue(helpTestIsNewer("11", "10", 4));
    assertTrue(helpTestIsNewer("11", "10", 5));

    assertFalse(helpTestIsNewer("11.1", "11.0", 1));
    assertTrue(helpTestIsNewer ("11.2", "11.1", 2));
    assertTrue(helpTestIsNewer ("11.3", "11.2", 3));
    assertTrue(helpTestIsNewer ("11.4", "11.3", 4));
    assertTrue(helpTestIsNewer ("11.5", "11.4", 5));

    assertFalse(helpTestIsNewer("11.0.1", "11.0.0", 1));
    assertFalse(helpTestIsNewer("11.0.2", "11.0.1", 2));
    assertTrue(helpTestIsNewer ("11.0.3", "11.0.2", 3));
    assertTrue(helpTestIsNewer ("11.0.4", "11.0.3", 4));
    assertTrue(helpTestIsNewer ("11.0.5", "11.0.4", 5));

    assertFalse(helpTestIsNewer("11.0.0.1", "11.0.0.0", 1));
    assertFalse(helpTestIsNewer("11.0.0.2", "11.0.0.1", 2));
    assertFalse(helpTestIsNewer("11.0.0.3", "11.0.0.2", 3));
    assertTrue(helpTestIsNewer ("11.0.0.4", "11.0.0.3", 4));
    assertTrue(helpTestIsNewer ("11.0.0.5", "11.0.0.4", 5));

    assertFalse(helpTestIsNewer("11.0.0.0.1", "11.0.0.0.0", 1));
    assertFalse(helpTestIsNewer("11.0.0.0.2", "11.0.0.0.1", 2));
    assertFalse(helpTestIsNewer("11.0.0.0.3", "11.0.0.0.2", 3));
    assertFalse(helpTestIsNewer("11.0.0.0.4", "11.0.0.0.3", 4));
    assertTrue(helpTestIsNewer ("11.0.0.0.5", "11.0.0.0.4", 5));

    assertFalse(helpTestIsNewer("1.1.1.1.1", "10.10.10.10.10", 1));
    assertFalse(helpTestIsNewer("1.1.1.1.1", "10.10.10.10.10", 2));
    assertFalse(helpTestIsNewer("1.1.1.1.1", "10.10.10.10.10", 3));
    assertFalse(helpTestIsNewer("1.1.1.1.1", "10.10.10.10.10", 4));
    assertFalse(helpTestIsNewer("1.1.1.1.1", "10.10.10.10.10", 5));

    assertTrue(helpTestIsNewer("10.7.0", "10.3.1.4.11", 3));
    assertFalse(helpTestIsNewer("10.3.1.4.11", "10.7.0", 3));
  }

  private boolean helpTestIsNewer(String s1, String s2, int depth) {
    Version v1 = new Version(s1);
    Version v2 = new Version(s2);
    return v1.isNewer(v2, depth);
  }
}
