/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;


public class SimpleTransactionManager implements PlatformTransactionManager {

  public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
    return new SimpleTransactionStatus();
  }

  public void commit(TransactionStatus status) throws TransactionException {
    // do nothing
  }

  public void rollback(TransactionStatus status) throws TransactionException {
    // do nothing
  }

}

