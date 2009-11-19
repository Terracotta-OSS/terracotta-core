/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import java.util.List;
import java.util.UUID;

public interface ILifeCycle {
  public UUID getSystemId();
  public void closeAppCtx();
  public List getInvocationRecords();
  public List getProp();
}
