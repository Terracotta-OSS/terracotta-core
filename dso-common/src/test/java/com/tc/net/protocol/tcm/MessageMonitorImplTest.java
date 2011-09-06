/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
