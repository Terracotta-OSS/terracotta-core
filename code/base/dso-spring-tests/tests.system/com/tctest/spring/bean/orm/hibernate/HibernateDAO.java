/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean.orm.hibernate;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;

public class HibernateDAO extends HibernateDaoSupport {
  AbstractPlatformTransactionManager transactionManager = null;

  public AbstractPlatformTransactionManager getTransactionManager() {
    return transactionManager;
  }

  public void setTransactionManager(AbstractPlatformTransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }
}
