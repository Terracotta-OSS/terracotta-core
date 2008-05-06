/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.objectserver.context.ManagedObjectRequestContext;

public interface ObjectRequestManager {
  
  public void requestObjects(ManagedObjectRequestContext responseContext, int maxReachableObjects);
  
}
