/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;

/**
 * Hooks into all subclasses of org.springframework.transaction.PlatformTransactionManager interface.
 * <p>
 * Interepts the start, commit, rollback TX methods.
 * <p>
 * It currently hijacks anything but NOT derivatives of the AbstractPlatformTransactionManager.
 * 
 * @author Jonas Bon&#233;r
 */
public class TransactionManagerProtocol {
  private static final String LOCK_NAME = "tc:spring-tx-lock";

  /**
   * TODO use <code>TransactionDefinition.isReadOnly()</code> to optimize lock types.
   */
  public Object startTransaction(final StaticJoinPoint jp, final Object manager) throws Throwable {
    try {
      return jp.proceed();
    } finally {
      ManagerUtil.beginLock(LOCK_NAME, Manager.LOCK_TYPE_WRITE);
    }
  }

  public Object commitTransaction(final StaticJoinPoint jp, final Object manager) throws Throwable {
    try {
      return jp.proceed();
    } finally {
      ManagerUtil.commitLock(LOCK_NAME);
    }
  }

  public Object rollbackTransaction(final StaticJoinPoint jp, final Object manager) throws Throwable {
    try {
      return jp.proceed();
    } finally {
      // FIXME rollback needs to be implemented in dso
      ManagerUtil.commitLock(LOCK_NAME);
    }
  }

}
