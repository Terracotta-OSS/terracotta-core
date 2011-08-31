/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.managedobject;

/**
 * This is just to glue things together in the server.
 */
public interface ManagedObjectChangeListenerProvider {
  public ManagedObjectChangeListener getListener();
}
