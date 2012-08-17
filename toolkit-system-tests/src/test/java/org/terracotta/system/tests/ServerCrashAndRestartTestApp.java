/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;

public class ServerCrashAndRestartTestApp extends ClientBase implements ClusterListener {
  private final ClusterInfo cluster;

  public ServerCrashAndRestartTestApp(final String[] args) {
    super(args);
    this.cluster = getClusteringToolkit().getClusterInfo();
    this.cluster.addClusterListener(this);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    final boolean isMasterNode = waitForAllClients() == 0;
    if (isMasterNode) {
      getTestControlMbean().crashActiveServer(0);
      Thread.sleep(5000);
      getTestControlMbean().reastartLastCrashedServer(0);
    }
    waitForAllClients();
  }

  public void nodeJoined(final ClusterEvent event) {
    trace("nodeJoined");
  }

  public void nodeLeft(final ClusterEvent event) {
    trace("nodeLeft");
  }

  public void operationsDisabled(final ClusterEvent event) {
    trace("operationsDisabled");
  }

  public void operationsEnabled(final ClusterEvent event) {
    trace("operationsEnabled");
  }

  private void trace(final String msg) {
    System.err.println("### " + msg + " -> nodeId=" + cluster.getCurrentNode().getId() + ", ThreadId="
                       + Thread.currentThread().getName());
  }

  @Override
  public void onClusterEvent(ClusterEvent event) {
    switch (event.getType()) {
      case OPERATIONS_DISABLED:
        operationsDisabled(event);
        break;
      case OPERATIONS_ENABLED:
        operationsEnabled(event);
        break;
      case NODE_JOINED:
        nodeJoined(event);
        break;
      case NODE_LEFT:
        nodeLeft(event);
        break;

    }

  }

}
