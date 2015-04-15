/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
