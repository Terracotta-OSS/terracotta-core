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

  @Override
  public void handleEvent(EventContext context) {
    AcknowledgeTransactionMessage atm = (AcknowledgeTransactionMessage) context;
    transactionManager.receivedAcknowledgement(atm.getLocalSessionID(), atm.getRequestID(), atm.getSourceNodeID());
  }

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext cc = (ClientConfigurationContext) context;
    transactionManager = cc.getTransactionManager();
  }
}