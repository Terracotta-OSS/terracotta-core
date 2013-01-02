/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.TransactionalObjectManager;

public class RecallObjectsHandler extends AbstractEventHandler implements EventHandler {

  private TransactionalObjectManager txnObjectMgr;

  @Override
  public void handleEvent(EventContext context) {
    RecallObjectsContext roc = (RecallObjectsContext) context;
    txnObjectMgr.recallCheckedoutObject(roc);
  }
  
  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    txnObjectMgr = oscc.getTransactionalObjectManager();
  }

}
