/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

public class TestObjectRequestManager implements ObjectRequestManager {

  public LinkedBlockingQueue<ObjectRequestServerContext> requestedObjects = new LinkedBlockingQueue<ObjectRequestServerContext>();

  public void requestObjects(ObjectRequestServerContext requestContext) {
    try {
      this.requestedObjects.put(requestContext);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

  }

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, boolean isPrefetched,
                          int maxRequestDepth) {
    // not implemented
  }
}
