/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapGetValueRequest;
import com.tc.object.ServerMapRequestID;
import com.tc.objectserver.core.api.ManagedObject;

import java.util.Collection;

public class NullServerMapRequestManager implements ServerMapRequestManager {

  public void requestSize(ServerMapRequestID requestID, ClientID clientID, ObjectID mapID) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public void requestValues(ClientID clientID, ObjectID mapID, Collection<ServerMapGetValueRequest> requests) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public void sendResponseFor(ObjectID mapID, ManagedObject managedObject) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

}
