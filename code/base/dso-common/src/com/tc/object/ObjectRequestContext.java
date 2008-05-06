/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.groups.ClientID;

import java.util.Set;

public interface ObjectRequestContext {

  public ObjectRequestID getRequestID();

  public ClientID getClientID();

  public Set getObjectIDs();

  public int getRequestDepth();

}