/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * A no-op implementation of the PlatformTransactionManager interface, to use when Spring's TX infrastructure is used to
 * handle locking in DSO but no underlying TX implementation is running. TODO how to register this manager in the di
 * config?
 * 
 * @author Jonas Bon&#233;r
 */
public class DsoTransactionManager implements PlatformTransactionManager {

  public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
    return new TransactionStatus() {
      public boolean isNewTransaction() {
        throw new UnsupportedOperationException();
      }

      public boolean hasSavepoint() {
        throw new UnsupportedOperationException();
      }

      public void setRollbackOnly() {
        throw new UnsupportedOperationException();
      }

      public boolean isRollbackOnly() {
        throw new UnsupportedOperationException();
      }

      public boolean isCompleted() {
        throw new UnsupportedOperationException();
      }

      public Object createSavepoint() throws TransactionException {
        throw new UnsupportedOperationException();
      }

      public void rollbackToSavepoint(Object savepoint) throws TransactionException {
        throw new UnsupportedOperationException();
      }

      public void releaseSavepoint(Object savepoint) throws TransactionException {
        throw new UnsupportedOperationException();
      }
    };
  }

  public void commit(TransactionStatus status) throws TransactionException {
    // noop
  }

  public void rollback(TransactionStatus status) throws TransactionException {
    // noop
  }
}
