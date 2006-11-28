/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;


public interface ServerTransactionManagerMBean {

  void addRootListener(ServerTransactionManagerEventListener listener);

}
