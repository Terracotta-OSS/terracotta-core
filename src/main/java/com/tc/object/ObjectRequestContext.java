/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.ClientID;

import java.util.Set;

public interface ObjectRequestContext {

  public ObjectRequestID getRequestID();

  public ClientID getClientID();

  public Set<EntityID> getRequestedEntities();
}
