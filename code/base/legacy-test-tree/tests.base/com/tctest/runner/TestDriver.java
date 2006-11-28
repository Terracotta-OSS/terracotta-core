/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.runner;

public interface TestDriver {
  
  public String[] getTestParameters();
  
  public void notifyError(Throwable t);

}
