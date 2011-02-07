/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.api;

/**
 * A transaction wrapper for the database transactions.
 */
public interface PersistenceTransaction {

  public Object getTransaction();

  public void commit();

  public void abort();
}
