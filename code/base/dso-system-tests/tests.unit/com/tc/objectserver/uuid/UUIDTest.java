/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.uuid;

import com.tc.test.TCTestCase;
import com.tc.util.UUID;

public class UUIDTest extends TCTestCase {

  public void testUUID() {
    // This test is located in one of the JDK1.5 specific source trees on purpose. If it is moved someplace where a 1.4
    // runtime will execute it, it will fail.

    String s = UUID.getUUID().toString();
    assertEquals(32, s.length());
    System.out.println(s);
  }

}
