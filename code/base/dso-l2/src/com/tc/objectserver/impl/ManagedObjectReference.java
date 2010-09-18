/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.object.cache.Cacheable;
import com.tc.objectserver.core.api.ManagedObject;

public interface ManagedObjectReference extends Cacheable {

  public void setRemoveOnRelease(boolean removeOnRelease);

  public boolean isRemoveOnRelease();

  public boolean markReference();

  public boolean unmarkReference();

  public boolean isReferenced();

  public boolean isNew();

  public ManagedObject getObject();

}
