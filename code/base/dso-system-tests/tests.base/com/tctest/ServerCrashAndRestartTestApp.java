/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.cluster.ClusterEventListener;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class ServerCrashAndRestartTestApp extends ServerCrashingAppBase implements ClusterEventListener {

  public ServerCrashAndRestartTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  private final int           initialNodeCount = getParticipantCount();
  private final CyclicBarrier barrier          = new CyclicBarrier(initialNodeCount);
  private String              thisNode;

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    ManagerUtil.addClusterEventListener(this);
    final boolean isMasterNode = barrier.barrier() == 0;
    if (isMasterNode) {
      getConfig().getServerControl().crash();
      getConfig().getServerControl().start(30 * 1000);
    }
    barrier.barrier();
  }

  public void nodeConnected(String nodeId) {
    trace("nodeConnected");
  }

  public void nodeDisconnected(String nodeId) {
    trace("nodeDisconnected");
  }

  public void thisNodeConnected(String thisNodeId, String[] nodesCurrentlyInCluster) {
    this.thisNode = thisNodeId;
    trace("thisNodeConnected");
  }

  public void thisNodeDisconnected(String thisNodeId) {
    trace("thisNodeDisconnected");
  }

  private void trace(String msg) {
    System.err.println("### " + msg + " -> nodeId=" + thisNode + ", ThreadId=" + Thread.currentThread().getName());
  }
}
