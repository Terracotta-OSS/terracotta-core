/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

public interface PersistenceTransaction {

  public Object getProperty(Object key);

  public Object setProperty(Object key, Object value);

  public void commit();
}
