/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin;

import com.tc.admin.model.ClusterModel;


public class StandardNodeFactory extends AbstractNodeFactory {
  @Override
  public ClusterNode createClusterNode(IAdminClientContext adminClientContext) {
    return new ClusterNode(adminClientContext);
  }

  @Override
  public ClusterNode createClusterNode(IAdminClientContext adminClientContext, String host, int jmxPort,
                                       boolean autoConnect) {
    return new ClusterNode(adminClientContext,new ClusterModel(host, jmxPort, autoConnect), autoConnect);
  }

}
