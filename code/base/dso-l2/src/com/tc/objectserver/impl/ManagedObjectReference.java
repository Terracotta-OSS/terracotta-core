/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.cache.Cacheable;
import com.tc.objectserver.core.api.ManagedObject;

// TODO:: Remove Cacheable interface from this interface.

public interface ManagedObjectReference extends Cacheable {

  public boolean getProcessPendingOnRelease();

  public void setProcessPendingOnRelease(boolean b);

  public void setRemoveOnRelease(boolean removeOnRelease);
  
  public boolean isRemoveOnRelease();

  public void markReference();

  public void unmarkReference();

  public boolean isReferenced();

  public boolean isNew();

  public ManagedObject getObject();
}
