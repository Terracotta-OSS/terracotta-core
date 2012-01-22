/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dmi;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

public class DmiClassSpecTest extends TestCase {

  private DmiClassSpec dcsA1;
  private DmiClassSpec dcsA2;
  private DmiClassSpec dcsB1;
  private DmiClassSpec dcsB2;

  public final void setUp() {
    final String s1 = "s1";
    final String s2 = "s2";
    dcsA1 = new DmiClassSpec(s1, s1);
    dcsA2 = new DmiClassSpec(s1, s1);
    dcsB1 = new DmiClassSpec(s1, s2);
    dcsB2 = new DmiClassSpec(s2, s1);
  }

  public void testHashCode() {
    final int hc = dcsA1.hashCode();
    assertTrue(hc == dcsA2.hashCode());
    assertFalse(hc == dcsB1.hashCode());
    assertFalse(hc == dcsB2.hashCode());
  }

  public void testEqualsObject() {
    assertTrue(dcsA1.equals(dcsA2));
    assertFalse(dcsA1.equals(dcsB1));
    assertFalse(dcsA1.equals(dcsB2));
  }
  
  public void testInSet() {
    final Set set = new HashSet();
    set.add(dcsA1);
    set.add(dcsA2);
    set.add(dcsB1);
    set.add(dcsB2);
    assertTrue(set.size() == 3);
  }

}
