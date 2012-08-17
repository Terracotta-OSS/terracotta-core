/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;


public interface L2DumperMBean {
  void doServerDump();

  int doThreadDump() throws Exception;

  void setThreadDumpCount(int count);

  void setThreadDumpInterval(long interval);

  void dumpClusterState();
}
