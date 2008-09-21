/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.net.NodeID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;

import java.util.Iterator;
import java.util.Set;

public class ServerTransactionAckHandler extends AbstractEventHandler {

  private ServerTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    ServerTxnAckMessage msg = (ServerTxnAckMessage) context;
    Set ackedTxns = msg.getAckedServerTxnIDs();
    NodeID waitee = msg.messageFrom();
    for (Iterator i = ackedTxns.iterator(); i.hasNext();) {
      ServerTransactionID sid = (ServerTransactionID) i.next();
      transactionManager.acknowledgement(sid.getSourceID(), sid.getClientTransactionID(), waitee);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.transactionManager = oscc.getTransactionManager();
  }

}
