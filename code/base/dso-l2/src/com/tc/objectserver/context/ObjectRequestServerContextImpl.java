/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.util.ObjectIDSet;

public class ObjectRequestServerContextImpl implements EventContext, ObjectRequestServerContext {

  private final ClientID        requestedNodeID;
  private final ObjectRequestID objectRequestID;
  private final ObjectIDSet     lookupIDs;
  private final String          requestingThreadName;
  private final int             requestDepth;
  private final boolean         serverInitiated;

  public ObjectRequestServerContextImpl(ClientID requestNodeID, ObjectRequestID objectRequestID, ObjectIDSet lookupIDs,
                                        String requestingThreadName, int requestDepth, boolean serverInitiated) {
    this.requestDepth = requestDepth;
    this.requestedNodeID = requestNodeID;
    this.objectRequestID = objectRequestID;
    this.lookupIDs = lookupIDs;
    this.requestingThreadName = requestingThreadName;
    this.serverInitiated = serverInitiated;
  }

  public ObjectIDSet getRequestedObjectIDs() {
    return lookupIDs;
  }

  public int getRequestDepth() {
    return requestDepth;
  }

  public ObjectRequestID getRequestID() {
    return objectRequestID;
  }

  public ClientID getClientID() {
    return requestedNodeID;
  }

  public String getRequestingThreadName() {
    return requestingThreadName;
  }

  public boolean isServerInitiated() {
    return serverInitiated;
  }

}
