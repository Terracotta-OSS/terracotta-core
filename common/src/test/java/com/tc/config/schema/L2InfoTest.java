/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.test.TCTestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class L2InfoTest extends TCTestCase {
  private final String  canonicalHostName;
  private final String  hostAddress;
  private final boolean haveFQHostName;

  public L2InfoTest() throws UnknownHostException {
    canonicalHostName = InetAddress.getLocalHost().getCanonicalHostName();
    hostAddress = InetAddress.getLocalHost().getHostAddress();
    haveFQHostName = !canonicalHostName.equals(hostAddress);
  }

  public void testEquals() {
    L2Info inst1 = new L2Info("primary", "localhost", 9520, 9510, null, 9530, null);
    L2Info inst2 = new L2Info("primary", "localhost", 9520, 9510, null,  9530, null);
    assertEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
    inst2 = new L2Info("secondary", "localhost", 9520, 9510, "",  9530, null);
    assertNotEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, "");
    inst2 = new L2Info("primary", "localhost", 9521, 9511, "",  9531, "");
    assertNotEquals(inst1, inst2);

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
    inst2 = new L2Info("primary", "127.0.0.1", 9520, 9510, "",  9530, null);
    assertNotEquals(inst1, inst2);
  }

  public void testMatch() {
    L2Info inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
    L2Info inst2 = new L2Info("primary", "127.0.0.1", 9520, 9510, "",  9530, null);
    assertTrue("hosts don't match", inst1.matches(inst2));

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
    inst2 = new L2Info("secondary", "localhost", 9520, 9510, "",  9530, null);
    assertFalse("names match", inst1.matches(inst2));

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
    inst2 = new L2Info("primary", "localhost", 9521, 9511, "",  9531, null);
    assertFalse("jmxPorts match", inst1.matches(inst2));

    inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
    inst2 = new L2Info("primary", "127.0.0.1", 9520, 9510, "",  9530, null);
    assertTrue("hosts don't match", inst1.matches(inst2));

    if (haveFQHostName) {
      inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
      inst2 = new L2Info("primary", canonicalHostName, 9520, 9510, "",  9530, null);
      assertTrue("hosts don't match", inst1.matches(inst2));

      inst1 = new L2Info("primary", "localhost", 9520, 9510, "",  9530, null);
      inst2 = new L2Info("primary", hostAddress, 9520, 9510, "",  9530, null);
      assertTrue("hosts don't match", inst1.matches(inst2));

      inst1 = new L2Info("primary", "127.0.0.1", 9520, 9510, "",  9530, null);
      inst2 = new L2Info("primary", hostAddress, 9520, 9510, "",  9530, null);
      assertTrue("hosts don't match", inst1.matches(inst2));
    }
  }
}
