/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.net.NodeID;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 * Base interface for application level messages sent through the TC messaging stack
 */
public interface TCAction {

  public TCMessageType getMessageType();

  /**
   * Hydrates the message with the given local session id.
   */
  public void hydrate() throws IOException, UnknownNameException;

  public NetworkRecall send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();
  
  public NodeID getDestinationNodeID();

  /**
   * The local session id is the session on the local side of the message bus. E.g., if this is a client, then it's the
   * client session id; likewise, if this is a server, then its the server session id.
   */
  public SessionID getLocalSessionID();
  /**
   * only for stats
   * @return
   */
  default int getMessageLength() {
    return 0;
  }
}
