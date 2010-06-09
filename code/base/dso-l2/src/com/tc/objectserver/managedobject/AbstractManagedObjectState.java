/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
   * This method returns whether this ManagedObjectState can have references or not. @ return true : The Managed object
   * represented by this state object will never have any reference to other objects. false : The Managed object
   * represented by this state object can have references to other objects.
   */
  public boolean hasNoReferences() {
    return false;
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
