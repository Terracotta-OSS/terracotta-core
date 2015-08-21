/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;


import junit.framework.TestCase;

public class MessageMonitorImplTest extends TestCase {
  
  public void tests() throws Exception {
    MessageMonitorImpl mm = new MessageMonitorImpl();
    mm.newIncomingMessage(new TestTCMessage());
    mm.newOutgoingMessage(new TestTCMessage());
    System.out.println(mm);
  }
}
