/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;

public class ClusterEventListener implements DsoClusterListener {

  private final RemoteTransactionManager txnMgr;

  public ClusterEventListener(RemoteTransactionManager remoteTransactionManager) {
    txnMgr = remoteTransactionManager;
  }

  @Override
  public void nodeJoined(DsoClusterEvent event) {

  }

  @Override
  public void nodeLeft(DsoClusterEvent event) {

  }

  @Override
  public void operationsEnabled(DsoClusterEvent event) {

  }

  @Override
  public void operationsDisabled(DsoClusterEvent event) {

  }

  @Override
  public void nodeRejoined(DsoClusterEvent event) {

  }

  @Override
  public void nodeError(DsoClusterEvent event) {
    txnMgr.requestImmediateShutdown();
  }

}