/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.l1.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.PostInit;
import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.objectserver.context.InvalidateObjectsForClientContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.InvalidateObjectManager;
import com.tc.util.concurrent.TCConcurrentMultiMap;

import java.util.Set;

/**
 * This class naturally batches invalidation to the clients by internally using a MultiMap
 */
public class InvalidateObjectManagerImpl implements InvalidateObjectManager, PostInit {

  private final TCConcurrentMultiMap<ClientID, ObjectID> invalidateMap = new TCConcurrentMultiMap<ClientID, ObjectID>(
                                                                                                                      256,
                                                                                                                      0.75f,
                                                                                                                      128);
  private Sink                                           invalidateSink;

  public void invalidateObjectFor(ClientID clientID, Set<ObjectID> oids) {
    if (invalidateMap.addAll(clientID, oids)) {
      invalidateSink.add(new InvalidateObjectsForClientContext(clientID));
    }
  }

  public Set<ObjectID> getObjectsIDsToInvalidate(ClientID clientID) {
    return invalidateMap.removeAll(clientID);
  }

  public void initializeContext(ConfigurationContext context) {
    this.invalidateSink = context.getStage(ServerConfigurationContext.INVALIDATE_OBJECTS_STAGE).getSink();
  }
}
