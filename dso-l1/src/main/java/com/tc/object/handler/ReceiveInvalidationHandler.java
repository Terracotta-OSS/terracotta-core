/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.ObjectID;
import com.tc.object.RemoteServerMapManager;
import com.tc.object.msg.InvalidateObjectsMessage;

public class ReceiveInvalidationHandler extends AbstractEventHandler implements EventHandler {

  private final RemoteServerMapManager remoteServerMapManager;

  public ReceiveInvalidationHandler(RemoteServerMapManager remoteServerMapManager) {
    this.remoteServerMapManager = remoteServerMapManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    InvalidateObjectsMessage invalidationContext = (InvalidateObjectsMessage) context;
    // invalidate all the objects by flushing them from the L1
    for (ObjectID oid : invalidationContext.getObjectIDsToInvalidate()) {
      remoteServerMapManager.flush(oid);
    }
  }

}
