/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.ImplementMe;
import com.tc.object.session.SessionID;

import junit.framework.TestCase;

public class MessageMonitorImplTest extends TestCase {
  
  public void tests() throws Exception {
    MessageMonitorImpl mm = new MessageMonitorImpl();
    mm.newIncomingMessage(new TestTCMessage());
    mm.newOutgoingMessage(new TestTCMessage());
    System.out.println(mm);
  }
  
  
  private static class TestTCMessage implements TCMessage {

    public int getCorrelationId(boolean initialize) {
      return 0;
    }

    public void setCorrelationId(int id) {
      return;
    }

    public TCMessageType getMessageType() {
      return TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE;
    }

    public void hydrate() {
      return;
    }

    public void dehydrate() {
      return;
    }

    public void send() {
      return;
    }

    public MessageChannel getChannel() {
      return null;
    }

    public int getTotalLength() {
      return 100;
    }

    public ChannelID getChannelID() {
      return null;
    }

    public boolean resend() {
      throw new ImplementMe();
      
    }

    public SessionID getLocalSessionID() {
      throw new ImplementMe();
    }
    
  }
}
