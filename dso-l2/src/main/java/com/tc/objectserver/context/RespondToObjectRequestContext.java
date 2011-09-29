/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.ObjectRequestServerContext.LOOKUP_STATE;
import com.tc.util.ObjectIDSet;

import java.util.Collection;

public interface RespondToObjectRequestContext extends EventContext {

  public ClientID getRequestedNodeID();

  public Collection getObjs();

  public ObjectIDSet getRequestedObjectIDs();

  public ObjectIDSet getMissingObjectIDs();

  public LOOKUP_STATE getLookupState();

  public int getRequestDepth();
}
