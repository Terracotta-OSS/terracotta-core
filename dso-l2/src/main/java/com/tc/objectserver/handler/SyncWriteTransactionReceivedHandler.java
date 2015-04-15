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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.SyncWriteTransactionReceivedMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.context.SyncWriteTransactionReceivedContext;

/**
 * This class is responsible for acking back to the clients when it receives a sync write transaction
 */
public class SyncWriteTransactionReceivedHandler extends AbstractEventHandler {
  private final DSOChannelManager channelManager;
  private final static TCLogger   logger = TCLogging.getLogger(SyncWriteTransactionReceivedHandler.class.getName());

  public SyncWriteTransactionReceivedHandler(DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    // send the message to the client
    SyncWriteTransactionReceivedContext syncCxt = (SyncWriteTransactionReceivedContext) context;
    ClientID cid = syncCxt.getClientID();
    long batchId = syncCxt.getBatchID();

    MessageChannel channel;
    try {
      channel = this.channelManager.getActiveChannel(cid);
    } catch (NoSuchChannelException e) {
      // Dont do anything
      logger.info("Cannot find channel for client " + cid + ". It might already be dead");
      return;
    }
    SyncWriteTransactionReceivedMessage message = (SyncWriteTransactionReceivedMessage) channel
        .createMessage(TCMessageType.SYNC_WRITE_TRANSACTION_RECEIVED_MESSAGE);
    message.initialize(batchId, syncCxt.getSyncTransactions());

    message.send();
  }
}
