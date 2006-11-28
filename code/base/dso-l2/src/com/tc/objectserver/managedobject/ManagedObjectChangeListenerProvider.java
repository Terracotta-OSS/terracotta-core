/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.managedobject;

/**
 * This is just to glue things together in the server.
 */
public interface ManagedObjectChangeListenerProvider {
  public ManagedObjectChangeListener getListener();
}
