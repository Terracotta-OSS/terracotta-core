/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import java.util.Arrays;

import junit.framework.TestCase;

/**
 * @author orion
 */
public class TCMessageTypeTest extends TestCase {
  public void testInitialization() {
    TCMessageType type = TCMessageType.getInstance(TCMessageType.TYPE_BROADCAST_TRANSACTION_MESSAGE);
    assertEquals(type.getType(), TCMessageType.TYPE_BROADCAST_TRANSACTION_MESSAGE);
    assertTrue(type.getTypeName() != null);

    System.out.println(Arrays.asList(TCMessageType.getAllMessageTypes()));
  }
}