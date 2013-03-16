/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

import java.util.Set;

public class ServerMapEvictionBroadcastContext implements EventContext {

  private final ObjectID mapOid;
  private final Set      evictedKeys;
  private final boolean  broadcastEvictions;

  public ServerMapEvictionBroadcastContext(final ObjectID mapOid, final Set evictedKeys) {
    this(mapOid, evictedKeys, false);
  }

  public ServerMapEvictionBroadcastContext(final ObjectID mapOid, final Set evictedKeys,
                                           final boolean broadcastEvictions) {
    this.mapOid = mapOid;
    this.evictedKeys = evictedKeys;
    this.broadcastEvictions = broadcastEvictions;
  }

  public ObjectID getMapOid() {
    return this.mapOid;
  }

  public Set getEvictedKeys() {
    return evictedKeys;
  }

  public boolean isBroadcastEvictions() {
    return broadcastEvictions;
  }
}
