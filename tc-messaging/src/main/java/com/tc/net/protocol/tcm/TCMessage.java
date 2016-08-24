/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.net.NodeID;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * Base interface for application level messages sent through the TC messaging stack
 */
public interface TCMessage {

  public TCMessageType getMessageType();

  /**
   * Hydrates the message with the given local session id.
   */
  public void hydrate() throws IOException, UnknownNameException;

  public void dehydrate();

  public boolean send();

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
