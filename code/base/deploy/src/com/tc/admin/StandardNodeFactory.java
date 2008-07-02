/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IServer;


public class StandardNodeFactory extends AbstractNodeFactory {
  public ClusterNode createClusterNode() {
    return new ClusterNode();
  }

  @Override
  public ClusterNode createClusterNode(String host, int jmxPort, boolean autoConnect) {
    return new ClusterNode(host, jmxPort, autoConnect);
  }

  @Override
  public ServerNode createServerNode(ServersNode serversNode, IServer server) {
    return new ServerNode(serversNode, server);
  }
  
}
