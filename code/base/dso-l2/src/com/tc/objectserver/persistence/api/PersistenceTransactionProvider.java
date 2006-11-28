/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.persistence.api;


public interface PersistenceTransactionProvider {
  public PersistenceTransaction newTransaction();
  public PersistenceTransaction nullTransaction();
}
