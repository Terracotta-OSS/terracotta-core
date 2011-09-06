/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.tx.ClientTransactionManager;

/**
 * Stage handler for processing transaction completed acknowledgment message at the L1
 */
public class ReceiveTransactionCompleteHandler extends AbstractEventHandler {
  private ClientTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    AcknowledgeTransactionMessage atm = (AcknowledgeTransactionMessage) context;
    transactionManager.receivedAcknowledgement(atm.getLocalSessionID(), atm.getRequestID(), atm.getSourceNodeID());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext cc = (ClientConfigurationContext) context;
    transactionManager = cc.getTransactionManager();
  }
}