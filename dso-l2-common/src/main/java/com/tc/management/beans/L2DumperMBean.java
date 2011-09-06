/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaMBean;

public interface L2DumperMBean extends TerracottaMBean {
  void doServerDump();

  int doThreadDump() throws Exception;

  void setThreadDumpCount(int count);

  void setThreadDumpInterval(long interval);

  void dumpClusterState();
}
