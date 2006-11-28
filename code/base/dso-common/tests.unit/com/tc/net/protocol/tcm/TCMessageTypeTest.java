/*
 * Created on Jul 9, 2004
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