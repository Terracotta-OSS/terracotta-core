/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.managedobject;

public class ManagedObjectChangeListenerProviderImpl implements ManagedObjectChangeListenerProvider {

  private ManagedObjectChangeListener listener;

  public void setListener(ManagedObjectChangeListener listener) {
    this.listener = listener;
  }
  
  public ManagedObjectChangeListener getListener() {
    if (this.listener == null) throw new AssertionError("Listener is null.");
    return this.listener;
  }

}
