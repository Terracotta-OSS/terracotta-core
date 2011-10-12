/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionInitiateContext;

public class ServerMapCapacityEvictionHandler extends AbstractEventHandler implements EventHandler {

  private final ServerMapEvictionManager serverMapEvictor;

  public ServerMapCapacityEvictionHandler(final ServerMapEvictionManager serverMapEvictor) {
    this.serverMapEvictor = serverMapEvictor;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ServerMapEvictionInitiateContext smec = (ServerMapEvictionInitiateContext) context;
    for (final ObjectID id : smec.getObjectIDs()) {
      this.serverMapEvictor.doEvictionOn(id, false);
    }
  }

}
