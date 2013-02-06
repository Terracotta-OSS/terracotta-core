/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

import com.google.common.eventbus.EventBus;
import com.tc.objectserver.core.api.ManagedObjectState;

/**
 * Base class for implementations of ManagedObjectState implementations.
 */
public abstract class AbstractManagedObjectState implements ManagedObjectState {

  public final EventBus getOperationEventBus() {
    return getStateFactory().getOperationEventBus();
  }

  public final ManagedObjectChangeListener getListener() {
    return getStateFactory().getListener();
  }

  public final ManagedObjectStateFactory getStateFactory() {
    return ManagedObjectStateFactory.getInstance();
  }

  /**
   * This is only for testing, its highly inefficient
   */
  @Override
  public final boolean equals(Object o) {
    if (o == null) return false;
    if (this == o) return true;
    if (getClass().getName().equals(o.getClass().getName())) { return basicEquals((AbstractManagedObjectState) o); }
    return false;
  }

  protected abstract boolean basicEquals(AbstractManagedObjectState o);

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
