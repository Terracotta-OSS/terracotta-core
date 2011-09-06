/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

import java.util.Set;

public class ServerMapEvictionInitiateContext implements EventContext {

  private final Set<ObjectID> serverMaps;

  public ServerMapEvictionInitiateContext(final Set<ObjectID> serverMaps) {
    this.serverMaps = serverMaps;
  }

  public Set<ObjectID> getObjectIDs() {
    return this.serverMaps;
  }

}
