/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.object.ObjectID;
import com.tc.objectserver.api.ServerMapEvictionManager;
import com.tc.objectserver.context.ServerMapEvictionInitiateContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.util.ObjectIDSet;

import java.util.SortedSet;

public class ServerMapCapacityEvictionHandler extends AbstractEventHandler implements EventHandler {

  private final ServerMapEvictionManager serverMapEvictor;
  private ClientStateManager             clientStateManager;

  public ServerMapCapacityEvictionHandler(final ServerMapEvictionManager serverMapEvictor) {
    this.serverMapEvictor = serverMapEvictor;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final ServerMapEvictionInitiateContext smec = (ServerMapEvictionInitiateContext) context;
    final SortedSet<ObjectID> faultedInClients = new ObjectIDSet();
    this.clientStateManager.addAllReferencedIdsTo(faultedInClients);
    for (final ObjectID id : smec.getObjectIDs()) {
      this.serverMapEvictor.doEvictionOn(id, faultedInClients, false);
    }
  }

  @Override
  protected void initialize(final ConfigurationContext context) {
    final ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.clientStateManager = scc.getClientStateManager();
  }
}
