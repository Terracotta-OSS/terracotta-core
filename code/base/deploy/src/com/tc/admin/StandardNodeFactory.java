/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.IClusterModel;
import com.tc.admin.model.IServerGroup;

public class StandardNodeFactory extends AbstractNodeFactory {
  @Override
  public ClusterNode createClusterNode(IAdminClientContext adminClientContext) {
    return new ClusterNode(adminClientContext);
  }

  @Override
  public ClusterNode createClusterNode(IAdminClientContext adminClientContext, String host, int jmxPort,
                                       boolean autoConnect) {
    return new ClusterNode(adminClientContext, host, jmxPort, autoConnect);
  }

  @Override
  public ServerGroupNode createServerGroupNode(IAdminClientContext adminClientContext, IClusterModel clusterModel,
                                               IServerGroup serverGroup) {
    return new ServerGroupNode(adminClientContext, clusterModel, serverGroup);
  }

}
