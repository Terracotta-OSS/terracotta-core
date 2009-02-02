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

public class TestObjectRequestManager implements ObjectRequestManager {

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth) {
    // not implemented
  }

  public void requestObjects(ObjectRequestServerContext requestContext) {
    // not implemented
  }
}
