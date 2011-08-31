/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.RemoteObjectManager;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectResponseMessage;

public class ReceiveObjectHandler extends AbstractEventHandler {
  private RemoteObjectManager objectManager;

  public void handleEvent(EventContext context) {
    if (context instanceof RequestManagedObjectResponseMessage) {
      RequestManagedObjectResponseMessage m = (RequestManagedObjectResponseMessage) context;
      objectManager.addAllObjects(m.getLocalSessionID(), m.getBatchID(), m.getObjects(), m.getSourceNodeID());
    } else {
      ObjectsNotFoundMessage notFound = (ObjectsNotFoundMessage) context;
      objectManager.objectsNotFoundFor(notFound.getLocalSessionID(), notFound.getBatchID(), notFound
          .getMissingObjectIDs(), notFound.getSourceNodeID());
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.objectManager = ccc.getObjectManager();
  }

}