/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
