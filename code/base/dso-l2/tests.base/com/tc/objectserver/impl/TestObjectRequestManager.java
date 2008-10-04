/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.object.ObjectRequestID;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.Collection;

public class TestObjectRequestManager implements ObjectRequestManager {

  public final NoExceptionLinkedQueue startCalls = new NoExceptionLinkedQueue();

  public void start() {
    startCalls.put(new Object());
  }

  public void requestObjects(ClientID clientID, ObjectRequestID requestID, ObjectIDSet ids, int maxRequestDepth,
                             boolean serverInitiated, String requestingThreadName) {
    // not implemented
  }

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {
    // not implemented
  }

}
