/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.mockl2.test;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.DsoClusterTopologyImpl;
import com.tc.exception.ImplementMe;
import com.tcclient.cluster.DsoNode;

public class MockDsoCluster implements DsoCluster {
  
  @Override
  public DsoNode waitUntilNodeJoinsCluster() {
    throw new ImplementMe();
  }
  
  @Override
  public void removeClusterListener(DsoClusterListener listener) {
    throw new ImplementMe();
    
  }
  
  @Override
  public boolean isNodeJoined() {
    throw new ImplementMe();
  }
  
  @Override
  public DsoNode getCurrentNode() {
    throw new ImplementMe();
  }
  
  @Override
  public DsoClusterTopology getClusterTopology() {
    MockUtil.logInfo("DSO Cluster : getClusterTopology");
    return new DsoClusterTopologyImpl();
  }
  
  @Override
  public boolean areOperationsEnabled() {
    throw new ImplementMe();
  }
  
  @Override
  public void addClusterListener(DsoClusterListener listener) {
 MockUtil.logInfo("DSO Cluster : add Cluster Listener");
    
  }
}
