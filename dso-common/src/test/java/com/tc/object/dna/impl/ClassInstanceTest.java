/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import junit.framework.TestCase;

public class ClassInstanceTest extends TestCase {

  public void testEquals() {
    ClassInstance c1 = new ClassInstance("name", "def");
    ClassInstance c2 = new ClassInstance("name", "def");
    ClassInstance c3 = new ClassInstance("def", "name");
    assertEquals(c1, c2);
    assertEquals(c1.hashCode(), c2.hashCode());
    assertFalse(c1.equals(c3));
    assertFalse(c3.equals(c1));
  }

}
