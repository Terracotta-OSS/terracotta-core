/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.net.protocol.TCNetworkMessage;
import com.tc.util.UUID;

/**
 * Message at the OAOO protocol level
 */
public interface OOOProtocolMessage extends TCNetworkMessage {

  public long getAckSequence();

  public long getSent();

  public boolean isHandshake();

  public boolean isHandshakeReplyOk();

  public boolean isHandshakeReplyFail();

  public boolean isSend();

  public boolean isAck();

  public boolean isGoodbye();

  public void reallyDoRecycleOnWrite();

  public UUID getSessionId();
}
