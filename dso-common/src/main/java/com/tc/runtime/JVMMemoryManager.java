/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

public interface JVMMemoryManager {
  
  public MemoryUsage getMemoryUsage();
  
  public MemoryUsage getOldGenUsage();

  public boolean isMemoryPoolMonitoringSupported();

}
