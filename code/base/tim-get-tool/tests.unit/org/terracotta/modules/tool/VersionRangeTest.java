/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool;

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
