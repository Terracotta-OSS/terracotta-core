/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

public class NullManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {
  public ManagedObjectChangeListener getListener() {
    return new NullManagedObjectChangeListener();
  }
}
