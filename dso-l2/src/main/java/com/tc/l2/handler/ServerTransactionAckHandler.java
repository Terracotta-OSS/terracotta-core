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
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.msg.ServerSyncTxnAckMessage;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;

import java.util.Iterator;
import java.util.Set;

public class ServerTransactionAckHandler extends AbstractEventHandler {

  private ServerTransactionManager transactionManager;
  private L2ObjectStateManager     l2ObjectStateManager;

  @Override
  public void handleEvent(EventContext context) {
    ServerTxnAckMessage msg = (ServerTxnAckMessage) context;
    Set ackedTxns = msg.getAckedServerTxnIDs();
    NodeID waitee = msg.messageFrom();
    for (Iterator i = ackedTxns.iterator(); i.hasNext();) {
      ServerTransactionID sid = (ServerTransactionID) i.next();
      transactionManager.acknowledgement(sid.getSourceID(), sid.getClientTransactionID(), waitee);
    }

    if (msg instanceof ServerSyncTxnAckMessage) {
      this.l2ObjectStateManager.ackSync(msg.messageFrom());
    }
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.transactionManager = oscc.getTransactionManager();
    this.l2ObjectStateManager = oscc.getL2Coordinator().getL2ObjectStateManager();
  }

}
