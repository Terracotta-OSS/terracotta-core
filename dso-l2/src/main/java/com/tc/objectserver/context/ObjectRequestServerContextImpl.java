/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;

import java.util.SortedSet;

public class ObjectRequestServerContextImpl implements ObjectRequestServerContext {

  private final ClientID            requestedNodeID;
  private final ObjectRequestID     objectRequestID;
  private final SortedSet<ObjectID> lookupIDs;
  private final String              requestingThreadName;
  private final int                 requestDepth;
  private final LOOKUP_STATE        lookupState;

  public ObjectRequestServerContextImpl(final ClientID requestNodeID, final ObjectRequestID objectRequestID,
                                        final SortedSet<ObjectID> lookupObjectIDs, final String requestingThreadName,
                                        final int requestDepth, final LOOKUP_STATE lookupState) {
    this.requestDepth = requestDepth;
    this.requestedNodeID = requestNodeID;
    this.objectRequestID = objectRequestID;
    this.lookupIDs = lookupObjectIDs;
    this.requestingThreadName = requestingThreadName;
    this.lookupState = lookupState;
  }

  /**
   * This is mutated outside, don't give a copy
   */
  public SortedSet<ObjectID> getRequestedObjectIDs() {
    return this.lookupIDs;
  }

  public int getRequestDepth() {
    return this.requestDepth;
  }

  public ObjectRequestID getRequestID() {
    return this.objectRequestID;
  }

  public ClientID getClientID() {
    return this.requestedNodeID;
  }

  public String getRequestingThreadName() {
    return this.requestingThreadName;
  }

  public LOOKUP_STATE getLookupState() {
    return this.lookupState;
  }

  public Object getKey() {
    return this.requestedNodeID;
  }
}
