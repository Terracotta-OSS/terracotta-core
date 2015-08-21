/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.cluster.Cluster;
import com.tc.operatorevent.NodeNameProvider;

public class ClientNameProvider implements NodeNameProvider {

  private final Cluster cluster;

  public ClientNameProvider(Cluster cluster) {
    this.cluster = cluster;
  }

  @Override
  public String getNodeName() {
    this.cluster.waitUntilNodeJoinsCluster();
    return this.cluster.getCurrentNode().getId();
  }

}
