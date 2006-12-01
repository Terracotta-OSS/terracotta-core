/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.runtime;

public interface JVMMemoryManager {
  
  public MemoryUsage getMemoryUsage();
  
  public MemoryUsage getOldGenUsage();

  public boolean isMemoryPoolMonitoringSupported();

}
