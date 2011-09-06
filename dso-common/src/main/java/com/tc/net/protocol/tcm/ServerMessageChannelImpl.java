/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.ServerID;
import com.tc.net.protocol.NetworkStackID;

/**
 * TODO: Document me
 * 
 * @author teck
 */
public class ServerMessageChannelImpl extends AbstractMessageChannel implements ServerMessageChannel {
  private static final TCLogger logger = TCLogging.getLogger(ServerMessageChannel.class);
  private final ChannelID       sessionID;

  /**
   * this is for the server it needs a session ID
   */
  protected ServerMessageChannelImpl(ChannelID sessionID, TCMessageRouter router, TCMessageFactory msgFactory,
                                     final ServerID serverID) {
    super(router, logger, msgFactory, new ClientID(sessionID.toLong()));
    this.sessionID = sessionID;
    setLocalNodeID(serverID);

    // server message channels should always be open initially
    synchronized (getStatus()) {
      channelOpened();
    }
  }

  public ChannelID getChannelID() {
    return sessionID;
  }

  @Override
  public NetworkStackID open() {
    throw new UnsupportedOperationException("Server channels don't support open()");
  }

}