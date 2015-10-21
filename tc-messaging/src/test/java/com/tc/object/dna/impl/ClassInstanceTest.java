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
package com.tc.object.dna.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ClassInstanceTest {

  @Test
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
