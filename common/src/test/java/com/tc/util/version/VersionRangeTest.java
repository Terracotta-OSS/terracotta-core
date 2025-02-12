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

import junit.framework.TestCase;

public class VersionRangeTest extends TestCase {

  public void testExact() {
    VersionRange r = new VersionRange("1.0.0-SNAPSHOT");
    assertTrue(r.isMinInclusive());
    assertEquals("1.0.0-SNAPSHOT", r.getMinVersion());
    assertEquals("1.0.0-SNAPSHOT", r.getMaxVersion());
    assertTrue(r.isMaxInclusive());
  }

  public void testExactPartial() {
    VersionRange r = new VersionRange("1.0");
    assertTrue(r.isMinInclusive());
    assertEquals("1.0", r.getMinVersion());
    assertEquals("1.0", r.getMaxVersion());
    assertTrue(r.isMaxInclusive());
  }

  public void testRange() {
    VersionRange r = new VersionRange("[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)");
    assertTrue(r.isMinInclusive());
    assertEquals("1.0.0-SNAPSHOT", r.getMinVersion());
    assertEquals("1.1.0-SNAPSHOT", r.getMaxVersion());
    assertFalse(r.isMaxInclusive());
  }

  public void testRange2() {
    VersionRange r = new VersionRange("(1.0.0-SNAPSHOT,1.1.0-SNAPSHOT]");
    assertFalse(r.isMinInclusive());
    assertEquals("1.0.0-SNAPSHOT", r.getMinVersion());
    assertEquals("1.1.0-SNAPSHOT", r.getMaxVersion());
    assertTrue(r.isMaxInclusive());
  }

  public void testOpenMaxRange() {
    VersionRange r = new VersionRange("[1.0.0-SNAPSHOT,]");
    assertEquals("1.0.0-SNAPSHOT", r.getMinVersion());
    assertEquals("", r.getMaxVersion());
    assertTrue(r.isMaxUnbounded());
    assertFalse(r.isMinUnbounded());
    assertTrue(r.isMaxInclusive());
    assertTrue(r.isMinInclusive());
    assertFalse(r.contains("0.0.1"));
    assertTrue(r.contains("1.0.0-SNAPSHOT"));
    assertTrue(r.contains("1.0.0"));
    assertTrue(r.contains("1.0.1"));
    assertTrue(r.contains("99.0.0"));
  }

  public void testOpenMinRange() {
    VersionRange r = new VersionRange("[,1.0.0-SNAPSHOT]");
    assertEquals("1.0.0-SNAPSHOT", r.getMaxVersion());
    assertEquals("", r.getMinVersion());
    assertFalse(r.isMaxUnbounded());
    assertTrue(r.isMinUnbounded());
    assertTrue(r.isMaxInclusive());
    assertTrue(r.isMinInclusive());
    assertTrue(r.contains("0.0.1"));
    assertTrue(r.contains("1.0.0-SNAPSHOT"));
    assertFalse(r.contains("1.0.0"));
  }

  public void testContainsMinInclusiveMaxExclusive() {
    VersionRange r = new VersionRange("[1.0.0,1.1.0)");
    assertFalse(r.contains("0.0.2"));
    assertFalse(r.contains("1.0.0-SNAPSHOT"));
    assertTrue(r.contains("1.0.0"));
    assertTrue(r.contains("1.0.1"));
    assertTrue(r.contains("1.0.1-SNAPSHOT"));
    assertTrue(r.contains("1.1.0-SNAPSHOT"));
    assertFalse(r.contains("1.1.0"));
    assertFalse(r.contains("1.1.1"));
  }

  public void testContainsMinInclusiveMaxExclusiveSnapshot() {
    VersionRange r = new VersionRange("[1.0.0-SNAPSHOT,1.1.0-SNAPSHOT)");
    assertFalse(r.contains("0.0.2"));
    assertTrue(r.contains("1.0.0-SNAPSHOT"));
    assertTrue(r.contains("1.0.1"));
    assertTrue(r.contains("1.0.1-SNAPSHOT"));
    assertFalse(r.contains("1.1.0-SNAPSHOT"));
    assertFalse(r.contains("1.1.0"));
    assertFalse(r.contains("1.1.1"));
  }

  public void testContainsMaxInclusiveMinExclusiveSnapshot() {
    VersionRange r = new VersionRange("(1.0.0-SNAPSHOT,1.1.0-SNAPSHOT]");
    assertFalse(r.contains("0.0.2"));
    assertFalse(r.contains("1.0.0-SNAPSHOT"));
    assertTrue(r.contains("1.0.1"));
    assertTrue(r.contains("1.0.1-SNAPSHOT"));
    assertTrue(r.contains("1.1.0-SNAPSHOT"));
    assertFalse(r.contains("1.1.0"));
    assertFalse(r.contains("1.1.1"));
  }
}
