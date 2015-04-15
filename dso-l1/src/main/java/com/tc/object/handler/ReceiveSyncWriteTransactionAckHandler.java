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
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.object.msg.SyncWriteTransactionReceivedMessage;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.TxnBatchID;

public class ReceiveSyncWriteTransactionAckHandler extends AbstractEventHandler {
  private final RemoteTransactionManager remoteTransactionManager;

  public ReceiveSyncWriteTransactionAckHandler(RemoteTransactionManager remoteTransactionManager) {
    this.remoteTransactionManager = remoteTransactionManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    SyncWriteTransactionReceivedMessage msg = (SyncWriteTransactionReceivedMessage) context;
    TxnBatchID batchID = new TxnBatchID(msg.getBatchID());
    NodeID nid = msg.getSourceNodeID();
    remoteTransactionManager.batchReceived(batchID, msg.getSyncTxnSet(), nid);
  }
}
