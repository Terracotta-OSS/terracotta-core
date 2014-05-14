/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.ClientID;

import java.util.SortedSet;

public interface ObjectRequestContext {

  public ObjectRequestID getRequestID();

  public ClientID getClientID();

  public SortedSet<ObjectID> getRequestedObjectIDs();

  public int getRequestDepth();

}
