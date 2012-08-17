/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.clusterinfo;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;

import com.tc.util.concurrent.ThreadUtil;

import junit.framework.Assert;

public class ClusterEventsBadClientsTestApp extends ClientBase {

  private final ToolkitAtomicLong root;
  private final ClusterInfo       clusterInfo;

  public ClusterEventsBadClientsTestApp(String[] args) {
    super(args);
    this.clusterInfo = getClusteringToolkit().getClusterInfo();
    this.root = getClusteringToolkit().getAtomicLong("testLong");
    clusterInfo.addClusterListener(new MyBadClusterListener(this.clusterInfo.getCurrentNode().getId()));
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {
    int i = 5;
    while (i-- > 0) {
      basicSetTesting();
      compareAndSetTesting();
      getAndSetTesting();
      System.out.println("XXX Iteration " + i + " done. sleeping for 30s");
      ThreadUtil.reallySleep(45 * 1000);
    }
  }

  private void initialize() throws Exception {
    int index = waitForAllClients();

    if (index == 0) {
      root.set(0);
    }

    waitForAllClients();
  }

  private void basicSetTesting() throws Exception {
    initialize();

    Assert.assertEquals(0, root.get());

    waitForAllClients();
  }

  private void compareAndSetTesting() throws Exception {
    initialize();

    int index = waitForAllClients();
    if (index == 0) {
      boolean set = root.compareAndSet(0, 100);
      if (!set) { throw new AssertionError("not set"); }
    }

    waitForAllClients();

    Assert.assertEquals(100, root.get());

    waitForAllClients();
  }

  private void getAndSetTesting() throws Exception {
    initialize();

    int index = waitForAllClients();
    if (index == 0) {
      long val = root.getAndSet(500);
      Assert.assertEquals(0, val);
    }

    waitForAllClients();

    Assert.assertEquals(500, root.get());

    waitForAllClients();
  }

  class MyBadClusterListener implements ClusterListener {

    private final String name;
    private int          i = 0;

    public MyBadClusterListener(String appId) {
      this.name = "Client[" + appId + "] : ";
    }

    public void nodeJoined(ClusterEvent dsoclusterevent) {
      System.out.println(name + "XXX NodeJoined: " + dsoclusterevent.getNode());
    }

    public void nodeLeft(ClusterEvent dsoclusterevent) {
      System.out.println(name + "XXX NodeLeft: " + dsoclusterevent.getNode());
    }

    public void operationsEnabled(ClusterEvent dsoclusterevent) {
      System.out.println(name + "XXX opsEnabled: " + dsoclusterevent.getNode());
    }

    public void operationsDisabled(ClusterEvent dsoclusterevent) {
      i++;
      if (i <= 1) {
        System.out.println(name + "XXX opsDisabled: " + dsoclusterevent.getNode() + "; checking exception handling");
        throw new RuntimeException("XXX I am a BAD man. I don't like ops disabled event.");
      } else {
        System.out.println(name + "XXX opsDisabled: " + dsoclusterevent.getNode()
                           + "; BLOCKING indefinite; checking L1 thread stuck behaviour");
        ThreadUtil.reallySleep(Integer.MAX_VALUE);
        // though the client cluster event handler stage is stuck, L1 still continues to rum and switch over to other
        // server and operate normally
      }
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

}
