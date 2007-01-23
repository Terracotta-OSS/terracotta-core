/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.object.TCObject;

public interface DistributedMethodCallManager {
  public void distributedInvoke(Object receiver, TCObject tcObject, String method, Object[] params);

  public void stop(boolean immediate);

}