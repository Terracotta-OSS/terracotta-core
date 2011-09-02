/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.ClientObjectManager;
import com.tc.object.TCObject;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.msg.ServerMapEvictionBroadcastMessage;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class ReceiveServerMapEvictionBroadcastHandler extends AbstractEventHandler {

  private static final boolean      EVICTOR_LOGGING = TCPropertiesImpl.getProperties()
                                                        .getBoolean(TCPropertiesConsts.EHCACHE_EVICTOR_LOGGING_ENABLED);
  private final ClientObjectManager clientObjectManager;

  public ReceiveServerMapEvictionBroadcastHandler(final ClientObjectManager clientObjectManager) {
    this.clientObjectManager = clientObjectManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ServerMapEvictionBroadcastMessage) {
      final ServerMapEvictionBroadcastMessage msg = (ServerMapEvictionBroadcastMessage) context;
      TCObject tco = null;
      tco = clientObjectManager.lookupIfLocal(msg.getMapID());
      if (tco == null || !(tco.getPeerObject() instanceof TCServerMap)) { return; }
      if (EVICTOR_LOGGING) {
        getLogger().info(
                         "Processing Server Map Eviction Broadcast msg Map OID=" + msg.getMapID() + " keys="
                             + msg.getEvictedKeys().size());
      }
      TCServerMap serverMap = (TCServerMap) tco.getPeerObject();
      for (Object key : msg.getEvictedKeys()) {
        serverMap.evictedInServer((msg.getClientIndex() == 0), key);
      }
    } else {
      throw new AssertionError("Unknown message type received from server - " + context.getClass().getName());
    }
  }
}
