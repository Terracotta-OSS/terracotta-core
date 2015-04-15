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
package com.tc.l2.objectserver;

import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ServerSyncTxnAckMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
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
  private final GroupManager                                  groupManager;
  private final ServerTransactionManager                      transactionManager;

  public L2ObjectSyncAckManagerImpl(final ServerTransactionManager transactionManager, final GroupManager groupManager) {
    this.transactionManager = transactionManager;
    this.groupManager = groupManager;
    transactionManager.addTransactionListener(this);
  }

  @Override
  public void reset() {
    txnsToAckMsgID.clear();
  }

  @Override
  public void addObjectSyncMessageToAck(final ServerTransactionID stxnID, final MessageID requestID) {
    if (txnsToAckMsgID.putIfAbsent(stxnID, requestID) != null) { throw new AssertionError("The same transaction "
                                                                                          + stxnID + " was sent twice"); }
  }

  @Override
  public void objectSyncComplete() {
    // TODO: run this as part of stateSyncManager.objectSyncComplete() after refactoring that a bit to take listeners
    transactionManager.callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionListener() {
      @Override
      public void onCompletion() {
        if (txnsToAckMsgID.size() != 0) { throw new AssertionError("Sync was not yet complete!"); }
        transactionManager.removeTransactionListener(L2ObjectSyncAckManagerImpl.this);
      }
    });
  }

  @Override
  public void ackObjectSyncTxn(final ServerTransactionID stxID) {
    MessageID msgID = txnsToAckMsgID.remove(stxID);
    if (msgID != null) {
      ServerSyncTxnAckMessage ackMsg = new ServerSyncTxnAckMessage(stxID.getSourceID(), msgID, Collections.singleton(stxID));
      try {
        groupManager.sendTo(stxID.getSourceID(), ackMsg);
      } catch (GroupException e) {
        groupManager.zapNode(stxID.getSourceID(), L2HAZapNodeRequestProcessor.COMMUNICATION_TO_ACTIVE_ERROR,
            "Failed to send object sync ack to active " + stxID.getSourceID() + ".");
      }
    }
  }

  @Override
  public void transactionCompleted(final ServerTransactionID stxID) {
    ackObjectSyncTxn(stxID);
  }
}
