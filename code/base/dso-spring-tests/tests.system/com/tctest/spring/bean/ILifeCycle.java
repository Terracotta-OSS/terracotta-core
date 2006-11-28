/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.List;

public interface ILifeCycle {
  public long getSystemId();
  public void closeAppCtx();
  public List getInvocationRecords();
  public List getProp();
}
