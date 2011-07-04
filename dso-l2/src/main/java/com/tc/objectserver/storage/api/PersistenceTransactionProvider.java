/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.storage.api;

/**
 * This class is responsible for creating PersistentTransaction.
 */
public interface PersistenceTransactionProvider {
  public PersistenceTransaction newTransaction();
}
