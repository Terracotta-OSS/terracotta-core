/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
