/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.ClientID;
import com.tc.object.ObjectRequestID;
import com.tc.util.ObjectIDSet;

import java.util.Collection;

public interface ObjectRequestManager {

  public void requestObjects(ClientID clientID, ObjectRequestID requestID, ObjectIDSet ids, int maxRequestDepth,
                             boolean serverInitiated, String requestingThreadName);

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs, ObjectIDSet missingObjectIDs,
                          boolean isServerInitiated, int maxRequestDepth);

}
