/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.managedobject;

public class NullManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {
  public ManagedObjectChangeListener getListener() {
    return new NullManagedObjectChangeListener();
  }
}
