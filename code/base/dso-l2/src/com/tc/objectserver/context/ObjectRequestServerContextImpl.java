/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectRequestID;
import com.tc.util.ObjectIDSet;

public class ObjectRequestServerContextImpl implements EventContext {

  private final ClientID requestedNodeID;
  private final ObjectRequestID objectRequestID;
  private final ObjectIDSet lookupIDs;
  private String requestingThreadName;
  
  public ObjectRequestServerContextImpl(ClientID requestNodeID, ObjectRequestID objectRequestID, ObjectIDSet lookupIDs, String requestingThreadName) {
    this.requestedNodeID = requestNodeID;
    this.objectRequestID = objectRequestID;
    this.lookupIDs = lookupIDs;
    this.requestingThreadName = requestingThreadName;
  }
  
  public ObjectIDSet getLookupIDs() {
    return lookupIDs;
  }

  public int getMaxRequestDepth() {
    return -1;
  }

  public ObjectRequestID getRequestID() {
    return objectRequestID;
  }

  public ClientID getRequestedNodeID() {
    return requestedNodeID;
  }

  public String getRequestingThreadName() {
    return requestingThreadName;
  }

  public boolean isServerInitiated() {
    return true;
  }

}
