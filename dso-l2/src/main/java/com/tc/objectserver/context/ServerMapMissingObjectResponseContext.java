/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.object.ObjectID;

public class ServerMapMissingObjectResponseContext implements EventContext {
  
  private final ObjectID mapID;

  public ServerMapMissingObjectResponseContext(final ObjectID mapID) {
    this.mapID = mapID;
  }

  public ObjectID getMapID() {
    return this.mapID;
  }

  @Override
  public String toString() {
    return "ServerMapMissingObjectResponseContext [ map : " + this.mapID + " ] ";
  }

}
