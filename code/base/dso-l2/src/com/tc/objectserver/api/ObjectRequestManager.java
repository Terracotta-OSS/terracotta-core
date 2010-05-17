/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.ClientID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.text.PrettyPrintable;
import com.tc.util.ObjectIDSet;

import java.util.Collection;

public interface ObjectRequestManager extends ObjectManagerMBean, PrettyPrintable {

  public void requestObjects(ObjectRequestServerContext requestContext);

  public void sendObjects(ClientID requestedNodeID, Collection objs, ObjectIDSet requestedObjectIDs,
                          ObjectIDSet missingObjectIDs, boolean isServerInitiated, int maxRequestDepth);

}
