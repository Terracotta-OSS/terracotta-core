/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * Base interface for application level messages sent through the TC messaging stack
 */
public interface TCMessage extends EventContext {

  public TCMessageType getMessageType();

  /**
   * Hydrates the message with the given local session id.
   */
  public void hydrate() throws IOException, UnknownNameException;

  public void dehydrate();

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();
  
  public NodeID getDestinationNodeID();

  /**
   * The local session id is the session on the local side of the message bus. E.g., if this is a client, then it's the
   * client session id; likewise, if this is a server, then its the server session id.
   */
  public SessionID getLocalSessionID();

  public int getTotalLength();

}