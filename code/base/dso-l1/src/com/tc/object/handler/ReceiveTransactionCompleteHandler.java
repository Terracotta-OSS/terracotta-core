/*
 * Created on Aug 25, 2004
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.msg.AcknowledgeTransactionMessage;
import com.tc.object.tx.ClientTransactionManager;

/**
 * @author steve
 */
public class ReceiveTransactionCompleteHandler extends AbstractEventHandler {
  private ClientTransactionManager transactionManager;

  public void handleEvent(EventContext context) {
    AcknowledgeTransactionMessage atm = (AcknowledgeTransactionMessage) context;
    transactionManager.receivedAcknowledgement(atm.getLocalSessionID(), atm.getRequestID());
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext cc = (ClientConfigurationContext) context;
    transactionManager = cc.getTransactionManager();
  }
}