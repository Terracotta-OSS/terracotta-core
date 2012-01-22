/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.exposed;

import com.tc.management.TerracottaMBean;

public interface TerracottaClusterMBean extends TerracottaMBean {

  boolean isConnected();

  String getNodeId();

  String[] getNodesInCluster();
}
