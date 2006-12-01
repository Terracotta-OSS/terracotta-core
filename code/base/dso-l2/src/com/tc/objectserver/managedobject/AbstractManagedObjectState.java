/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.tc.objectserver.core.api.ManagedObjectState;

/**
 * Base class for implementations of ManagedObjectState implementations.
 */
public abstract class AbstractManagedObjectState implements ManagedObjectState {

  public final ManagedObjectChangeListener getListener() {
    return getStateFactory().getListener();
  }

  public final ManagedObjectStateFactory getStateFactory() {
    return ManagedObjectStateFactory.getInstance();
  }

  /**
   * This is only for testing, its highly inefficient
   */
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (getClass().getName().equals(o.getClass().getName())) { return basicEquals((AbstractManagedObjectState) o); }
    return false;
  }

  protected abstract boolean basicEquals(AbstractManagedObjectState o);

}
