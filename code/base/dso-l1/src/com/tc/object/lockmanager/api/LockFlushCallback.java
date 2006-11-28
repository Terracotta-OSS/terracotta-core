/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.lockmanager.api;

public interface LockFlushCallback {

  /* This method is called when all the transactions made under that Lock are ACKED */
  public void transactionsForLockFlushed(LockID id);
}
