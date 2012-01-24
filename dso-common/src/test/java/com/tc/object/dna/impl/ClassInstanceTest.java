/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import junit.framework.TestCase;

public class ClassInstanceTest extends TestCase {

  public void testEquals() {
    ClassInstance c1 = new ClassInstance("name");
    ClassInstance c2 = new ClassInstance("name");
    ClassInstance c3 = new ClassInstance("def");
    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
    assertFalse(c1.equals(c3));
    assertFalse(c3.equals(c1));
  }

}
