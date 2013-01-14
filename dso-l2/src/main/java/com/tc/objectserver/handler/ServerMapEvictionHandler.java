/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionContext;

public class ServerMapEvictionHandler extends AbstractEventHandler {

  private final ServerMapEvictionManager serverMapEvictor;

  public ServerMapEvictionHandler(final ServerMapEvictionManager serverMapEvictor) {
    this.serverMapEvictor = serverMapEvictor;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ServerMapEvictionContext smec = (ServerMapEvictionContext) context;
    this.serverMapEvictor.evict(smec.getOid(), smec.getRandomSamples(), smec.getClassName(), smec.getCacheName());
  }

}
