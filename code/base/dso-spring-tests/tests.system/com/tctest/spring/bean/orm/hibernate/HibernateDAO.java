/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
