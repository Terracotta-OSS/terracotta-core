/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.TerracottaMBean;

public interface ObjectManagementMonitorMBean extends TerracottaMBean {
  
  void runGC();
  
  boolean isGCRunning();
  
}
