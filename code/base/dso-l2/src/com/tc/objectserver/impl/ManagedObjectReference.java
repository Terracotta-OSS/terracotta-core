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

  /**
   * Pins this reference in the cache.
   */
  public void pin();

  public void unpin();

  /**
   * Determines whether or not this reference is pinned in the ObjectManager's cache. This allows the object manager to
   * lookup multiple objects one at a time without evicting them from the cache.
   */
  public boolean isPinned();

  public ManagedObject getObject();
}
