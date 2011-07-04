/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.net.GroupID;

public interface ClusterInfo {

  boolean hasServerInCluster(String name);

  GroupID getGroupIDFromServerName(String name);

  boolean hasServerInGroup(String serverName);
}