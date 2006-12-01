/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

public interface LockFlushCallback {

  /* This method is called when all the transactions made under that Lock are ACKED */
  public void transactionsForLockFlushed(LockID id);
}
