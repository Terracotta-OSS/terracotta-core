/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.exposed;

import com.tc.management.TerracottaMBean;

public interface TerracottaClusterMBean extends TerracottaMBean {

  boolean isConnected();

  String getNodeId();

  String[] getNodesInCluster();
}
