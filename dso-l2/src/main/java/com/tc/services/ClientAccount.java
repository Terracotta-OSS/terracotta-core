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

package com.tc.services;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;


public class ClientAccount {
  private final ClientID clientID;
  private final ClientMessageSender sender;
  private final MessageChannel channel;
  private volatile boolean open = true;

  ClientAccount(ClientMessageSender sender, MessageChannel channel) {
    this.sender = sender;
    this.clientID = (ClientID)channel.getRemoteNodeID();
    this.channel = channel;
  }

  synchronized void sendNoResponse(ClientInstanceID clientInstance, byte[] payload) {
    if (open) {
      this.sender.send(this.clientID, clientInstance, payload);
    }
  }

  synchronized void sendInvokeMessage(TransactionID transaction, byte[] payload) {
    if (open) {
      this.sender.send(this.clientID, transaction, payload);
    }
  }
  /** 
   * going to initiate the close here.  also want to shutdown all the waiters because 
   * the mapping is going to be removed from above
   */

  synchronized void close() {
    if (channel.isOpen()) {
      // if the channel is open, this means that a consumer of the ClientCommunicator API
      // has directly requested a close on the client connection.  This slightly reorders things
      // but it shouldn't matter here.  waiting on response is not something that is really supported anyways
      // (API is deprecated)
      channel.close();
    }
    open = false;
  }
}
