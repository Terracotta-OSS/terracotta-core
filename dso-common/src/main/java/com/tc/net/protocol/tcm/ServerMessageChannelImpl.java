/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol.tcm;

import com.tc.license.ProductID;
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
                                     final ServerID serverID, final ProductID productId) {
    super(router, logger, msgFactory, new ClientID(sessionID.toLong()), productId);
    this.sessionID = sessionID;
    setLocalNodeID(serverID);

    // server message channels should always be open initially
    synchronized (getStatus()) {
      channelOpened();
    }
  }

  @Override
  public ChannelID getChannelID() {
    return sessionID;
  }

  @Override
  public NetworkStackID open() {
    throw new UnsupportedOperationException("Server channels don't support open()");
  }

  @Override
  public NetworkStackID open(char[] password) {
    throw new UnsupportedOperationException("Server channels don't support open()");
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Server channels don't support reset()");
  }

}