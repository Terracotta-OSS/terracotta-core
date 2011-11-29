/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.Sink;
import com.tc.l2.msg.ServerSyncTxnAckMessage;
import com.tc.l2.msg.ServerTxnAckMessageFactory;
import com.tc.net.groups.MessageID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.tx.AbstractServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TxnsInSystemCompletionListener;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class L2ObjectSyncAckManagerImpl extends AbstractServerTransactionListener implements L2ObjectSyncAckManager {
  private final ConcurrentMap<ServerTransactionID, MessageID> txnsToAckMsgID = new ConcurrentHashMap<ServerTransactionID, MessageID>();
  private final Sink                                          sendSink;
  private final ServerTransactionManager                      transactionManager;

  public L2ObjectSyncAckManagerImpl(final Sink sendSink, final ServerTransactionManager transactionManager) {
    this.sendSink = sendSink;
    this.transactionManager = transactionManager;
    transactionManager.addTransactionListener(this);
  }

  public void reset() {
    txnsToAckMsgID.clear();
  }

  public void addObjectSyncMessageToAck(final ServerTransactionID stxnID, final MessageID requestID) {
    if (txnsToAckMsgID.putIfAbsent(stxnID, requestID) != null) { throw new AssertionError("The same transaction "
                                                                                          + stxnID + " was sent twice"); }
  }

  public void objectSyncComplete() {
    // TODO: run this as part of stateSyncManager.objectSyncComplete() after refactoring that a bit to take listeners
    transactionManager.callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      public void onCompletion() {
        if (txnsToAckMsgID.size() != 0) { throw new AssertionError("Sync was not yet complete!"); }
        transactionManager.removeTransactionListener(L2ObjectSyncAckManagerImpl.this);
      }
    });
  }

  public void ackObjectSyncTxn(final ServerTransactionID stxID) {
    MessageID msgID = txnsToAckMsgID.remove(stxID);
    if (msgID != null) {
      ServerSyncTxnAckMessage ackMsg = ServerTxnAckMessageFactory.createServerSyncTxnAckMessage(stxID.getSourceID(),
                                                                                                msgID, Collections
                                                                                                    .singleton(stxID));
      sendSink.add(ackMsg);
    }
  }

  @Override
  public void transactionCompleted(final ServerTransactionID stxID) {
    ackObjectSyncTxn(stxID);
  }

  public void removeAckForTxn(final ServerTransactionID stxnID) {
    txnsToAckMsgID.remove(stxnID);
  }
}
