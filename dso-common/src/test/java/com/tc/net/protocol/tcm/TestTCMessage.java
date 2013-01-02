/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;

public class TestTCMessage implements TCMessage {

  public TCMessageType type = TCMessageType.PING_MESSAGE;

  public int getCorrelationId(boolean initialize) {
    return 0;
  }

  public void setCorrelationId(int id) {
    return;
  }

  @Override
  public TCMessageType getMessageType() {
    return type;
  }

  @Override
  public void hydrate() {
    return;
  }

  @Override
  public void dehydrate() {
    return;
  }

  @Override
  public void send() {
    return;
  }

  @Override
  public MessageChannel getChannel() {
    return null;
  }

  @Override
  public int getTotalLength() {
    return 100;
  }

  public ClientID getClientID() {
    return null;
  }

  public boolean resend() {
    throw new ImplementMe();

  }

  @Override
  public SessionID getLocalSessionID() {
    throw new ImplementMe();
  }

  @Override
  public NodeID getSourceNodeID() {
    throw new ImplementMe();
  }

  @Override
  public NodeID getDestinationNodeID() {
    throw new ImplementMe();
  }

}
