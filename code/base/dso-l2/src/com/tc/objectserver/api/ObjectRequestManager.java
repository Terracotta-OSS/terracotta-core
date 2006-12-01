/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.objectserver.context.ManagedObjectRequestContext;

import java.util.Collection;

public interface ObjectRequestManager {
  
  public void requestObjects(Collection ids, ManagedObjectRequestContext responseContext, int maxReachableObjects);
  
}
