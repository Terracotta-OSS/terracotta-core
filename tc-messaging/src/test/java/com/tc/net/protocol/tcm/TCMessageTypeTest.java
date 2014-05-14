/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author orion
 */
public class TCMessageTypeTest {
  
  @Test
  public void testInitialization() {
    TCMessageType type = TCMessageType.getInstance(TCMessageType.TYPE_BROADCAST_TRANSACTION_MESSAGE);
    assertEquals(type.getType(), TCMessageType.TYPE_BROADCAST_TRANSACTION_MESSAGE);
    assertTrue(type.getTypeName() != null);
  }
}
