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
package com.tc.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UUIDTest {

  @Test
  public void testUUID() {
    // This test is located in one of the JDK1.5 specific source trees on purpose. If it is moved someplace where a 1.4
    // runtime will execute it, it will fail.

    String s = UUID.getUUID().toString();
    assertEquals(32, s.length());
  }

}
