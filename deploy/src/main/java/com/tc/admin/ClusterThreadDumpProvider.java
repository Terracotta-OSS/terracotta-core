/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

public interface ClusterThreadDumpProvider {
  ClusterThreadDumpEntry takeThreadDump();

  ClusterThreadDumpEntry takeClusterDump();
}
