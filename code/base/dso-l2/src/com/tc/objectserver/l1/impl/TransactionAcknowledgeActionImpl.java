/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.ServerTransactionID;

/**
 * @author steve
 */
public class TransactionAcknowledgeActionImpl implements TransactionAcknowledgeAction {
  private final DSOChannelManager channelManager;
  private final TCLogger          logger = TCLogging.getLogger(TransactionAcknowledgeActionImpl.class);

  public TransactionAcknowledgeActionImpl(DSOChannelManager channelManager) {
    this.channelManager = channelManager;
  }

  public void acknowledgeTransaction(ServerTransactionID stxID) {
    try {
      MessageChannel channel = channelManager.getChannel(stxID.getChannelID());
      AcknowledgeTransactionMessage m = (AcknowledgeTransactionMessage) channel
          .createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
      m.initialize(stxID.getChannelID(), stxID.getClientTransactionID());
      m.send();
    } catch (NoSuchChannelException e) {
      logger.info("An attempt was made to send a commit ack but the client seems to have gone away:"
                  + stxID );
      return;
    }
  }
}