/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.msg.BatchTransactionAcknowledgeMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.objectserver.tx.NoSuchBatchException;
import com.tc.objectserver.tx.TransactionBatchManager;

/**
 * @author steve
 */
public class TransactionAcknowledgeActionImpl implements TransactionAcknowledgeAction {
  private final DSOChannelManager       channelManager;
  private final TCLogger                logger = TCLogging.getLogger(TransactionAcknowledgeActionImpl.class);
  private final TransactionBatchManager transactionBatchManager;

  public TransactionAcknowledgeActionImpl(DSOChannelManager channelManager,
                                          TransactionBatchManager transactionBatchManager) {
    this.channelManager = channelManager;
    this.transactionBatchManager = transactionBatchManager;
  }

  public void acknowledgeTransaction(ServerTransactionID stxID) {
    try {
      NodeID nodeID = stxID.getSourceID();
      MessageChannel channel = channelManager.getActiveChannel(nodeID);

      // send ack
      AcknowledgeTransactionMessage m = (AcknowledgeTransactionMessage) channel
          .createMessage(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE);
      m.initialize(stxID.getSourceID(), stxID.getClientTransactionID());
      m.send();

      // send batch ack if necessary
      try {
        if (transactionBatchManager.batchComponentComplete(nodeID, stxID.getClientTransactionID())) {
          BatchTransactionAcknowledgeMessage msg = channelManager.newBatchTransactionAcknowledgeMessage(nodeID);
          // We always send null batch ID since its never used - clean up
          msg.initialize(TxnBatchID.NULL_BATCH_ID);
          msg.send();
        }
      } catch (NoSuchBatchException e) {
        throw new AssertionError(e);
      }

    } catch (NoSuchChannelException e) {
      logger.info("An attempt was made to send a commit ack but the client seems to have gone away:" + stxID);
      return;
    }
  }
}