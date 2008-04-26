/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema;

import com.tc.test.TCTestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class L2InfoTest extends TCTestCase {
  private String  canonicalHostName;
  private String  hostAddress;
  private boolean haveFQHostName;

  public L2InfoTest() {
    try {
      canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
      hostAddress = InetAddress.getLocalHost().getHostAddress();
      haveFQHostName = !canonicalHostName.equals(hostAddress);
    } catch (UnknownHostException uhe) {
      disableAllUntil("2010-01-01");
    }
  }

  public void testEquals() {
    L2Info inst1 = new L2Info("primary", "localhost", 9520);
    L2Info inst2 = new L2Info("primary", "localhost", 9520);
    assertEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520);
    inst2 = new L2Info("secondary", "localhost", 9520);
    assertNotEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520);
    inst2 = new L2Info("primary", "localhost", 9521);
    assertNotEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520);
    inst2 = new L2Info("primary", "127.0.0.1", 9520);
    assertNotEquals(inst1, inst2);
  }

  public void testMatch() {
    L2Info inst1 = new L2Info("primary", "localhost", 9520);
    L2Info inst2 = new L2Info("primary", "127.0.0.1", 9520);
    assertTrue("hosts don't match", inst1.matches(inst2));

    inst1 = new L2Info("primary", "localhost", 9520);
    inst2 = new L2Info("secondary", "localhost", 9520);
    assertFalse("names match", inst1.matches(inst2));

    inst1 = new L2Info("primary", "localhost", 9520);
    inst2 = new L2Info("primary", "localhost", 9521);
    assertFalse("jmxPorts match", inst1.matches(inst2));

    inst1 = new L2Info("primary", "localhost", 9520);
    inst2 = new L2Info("primary", "127.0.0.1", 9520);
    assertTrue("hosts don't match", inst1.matches(inst2));

    if (haveFQHostName) {
      inst1 = new L2Info("primary", "localhost", 9520);
      inst2 = new L2Info("primary", canonicalHostName, 9520);
      assertTrue("hosts don't match", inst1.matches(inst2));

      inst1 = new L2Info("primary", "localhost", 9520);
      inst2 = new L2Info("primary", hostAddress, 9520);
      assertTrue("hosts don't match", inst1.matches(inst2));

      inst1 = new L2Info("primary", "127.0.0.1", 9520);
      inst2 = new L2Info("primary", hostAddress, 9520);
      assertTrue("hosts don't match", inst1.matches(inst2));
    }
  }
}
