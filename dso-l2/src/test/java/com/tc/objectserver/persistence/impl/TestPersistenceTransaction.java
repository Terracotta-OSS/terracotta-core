/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.impl;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

import com.tc.exception.TCRuntimeException;
import com.tc.objectserver.storage.api.PersistenceTransaction;

public final class TestPersistenceTransaction implements PersistenceTransaction {

  public static final TestPersistenceTransaction NULL_TRANSACTION   = new TestPersistenceTransaction();

  public final LinkedQueue                       commitContexts     = new LinkedQueue();
  public final LinkedQueue                       commitSyncContexts = new LinkedQueue();

  public void commit() {
    try {
      commitContexts.put(new Object());
    } catch (InterruptedException e) {
      throw new TCRuntimeException(e);
    }
  }

  public void abort() {
    throw new UnsupportedOperationException();
  }

  public Object getTransaction() {
    return null;
  }
}