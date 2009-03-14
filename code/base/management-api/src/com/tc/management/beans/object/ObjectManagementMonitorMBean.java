/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.beans.object;

import com.tc.management.TerracottaMBean;

import java.util.Set;

public interface ObjectManagementMonitorMBean extends TerracottaMBean {
  
  void runGC();
  
  boolean isGCRunning();
  
  Set getAllObjectIds();
}
