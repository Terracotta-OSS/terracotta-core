/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.util.version;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class VersionTest {

  private void helpTestParse(String str, int major, int minor, int micro, int patch, int build, String specifier, String qualifier) {
    Version v = new Version(str);
    assertEquals(major, v.major(), "major");
    assertEquals(minor, v.minor(), "minor");
    assertEquals(micro, v.micro(), "micro");
    assertEquals(specifier, v.specifier(), "specifier");
    assertEquals(qualifier, v.qualifier(), "qualifier");
  }
  
  @Test
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
  
  @Test
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
      assertTrue(compared == 0, ()->"expected 0, got: " + compared);
      assertTrue(comparedBackwards == 0);
    } else if(compareDirection < 0) {
      assertTrue(compared < 0);
      assertTrue(comparedBackwards > 0);
    } else {
      assertTrue(compared > 0);
      assertTrue(comparedBackwards < 0);
    }
  }

  @Test
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

  @Test
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
}
