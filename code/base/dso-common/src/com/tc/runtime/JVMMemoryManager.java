/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.runtime;

public interface JVMMemoryManager {
  
  public MemoryUsage getMemoryUsage();
  
  public MemoryUsage getOldGenUsage();

  public boolean isMemoryPoolMonitoringSupported();

}
