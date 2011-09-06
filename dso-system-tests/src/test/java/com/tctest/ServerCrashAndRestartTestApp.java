/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import EDU.oswego.cs.dl.util.concurrent.CyclicBarrier;

import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;

public class ServerCrashAndRestartTestApp extends ServerCrashingAppBase implements DsoClusterListener {

  public ServerCrashAndRestartTestApp(final String appId, final ApplicationConfig config,
                                      final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
  }

  @InjectedDsoInstance
  private DsoCluster          cluster;
  private final int           initialNodeCount = getParticipantCount();
  private final CyclicBarrier barrier          = new CyclicBarrier(initialNodeCount);

  @Override
  public void runTest() throws Throwable {
    cluster.addClusterListener(this);
    final boolean isMasterNode = barrier.barrier() == 0;
    if (isMasterNode) {
      getConfig().getServerControl().crash();
      getConfig().getServerControl().start();
    }
    barrier.barrier();
  }

  public void nodeJoined(final DsoClusterEvent event) {
    trace("nodeJoined");
  }

  public void nodeLeft(final DsoClusterEvent event) {
    trace("nodeLeft");
  }

  public void operationsDisabled(final DsoClusterEvent event) {
    trace("operationsDisabled");
  }

  public void operationsEnabled(final DsoClusterEvent event) {
    trace("operationsEnabled");
  }

  private void trace(final String msg) {
    System.err.println("### " + msg + " -> nodeId=" + cluster.getCurrentNode().getId() + ", ThreadId="
                       + Thread.currentThread().getName());
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    config.addIncludePattern(ServerCrashAndRestartTestApp.class.getName());
    config.addRoot("barrier", ServerCrashAndRestartTestApp.class.getName() + ".barrier");
    config.addWriteAutolock("* " + ServerCrashAndRestartTestApp.class.getName() + ".*(..)");

    config.addIncludePattern(CyclicBarrier.class.getName());
    config.addWriteAutolock("* " + CyclicBarrier.class.getName() + ".*(..)");
  }
}
