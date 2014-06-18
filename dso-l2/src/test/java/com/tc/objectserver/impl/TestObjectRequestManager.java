/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;

public class TestObjectRequestManager implements ObjectRequestManager {

  public LinkedBlockingQueue<ObjectRequestServerContext> requestedObjects = new LinkedBlockingQueue<ObjectRequestServerContext>();

  @Override
  public void requestObjects(ObjectRequestServerContext requestContext) {
    try {
      this.requestedObjects.put(requestContext);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

  }

  @Override
  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, LOOKUP_STATE lookupState, int maxRequestDepth) {
    // not implemented
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }

  public int getCachedObjectCount() {
    return 0;
  }

  @Override
  public int getLiveObjectCount() {
    return 0;
  }

  @Override
  public Iterator getRootNames() {
    return null;
  }

  @Override
  public Iterator getRoots() {
    return null;
  }

  @Override
  public ObjectID lookupRootID(String name) {
    return null;
  }
}
