/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.clusterinfo;

import org.terracotta.express.tests.CallableWaiter;
import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterInfo;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitMap;

import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.ServerCrashMode;
import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.Assert;

public class ClusterInfoTest extends AbstractToolkitTestBase {

  public ClusterInfoTest(TestConfig testConfig) {
    super(testConfig, ClusterInfoTestApp.class, ClusterInfoTestApp.class, ClusterInfoTestApp.class);
    testConfig.setNumOfGroups(1);
    testConfig.getGroupConfig().setMemberCount(2);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.TEMPORARY_SWAP_ONLY);
    testConfig.getCrashConfig().setCrashMode(ServerCrashMode.NO_CRASH);
  }

  public static class ClusterInfoTestApp extends ClientBase {
    public static void main(String[] args) {
      new ClusterInfoTestApp(args).run();
    }

    public ClusterInfoTestApp(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      final ClusterInfo info = toolkit.getClusterInfo();
      ClusterNode node = info.getCurrentNode();

      Assert.assertTrue(node.equals(info.getCurrentNode()));

      Assert.assertTrue(info.areOperationsEnabled());

      Assert.assertTrue(info.areOperationsEnabled());

      CallableWaiter.waitOnCallable(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          int size = info.getNodes().size();
          debug("Waiting for all 3 nodes, got: " + size);
          return size == 3;
        }
      });

      Assert.assertTrue(info.getNodes().contains(info.getCurrentNode()));

      ToolkitMap<String, String> map = toolkit.getMap("testMap", null, null);
      map.put(node.getId(), "yup");
      getBarrierForAllClients().await();
      Assert.assertEquals(3, map.size());

      final AtomicInteger nodeJoinedCount = new AtomicInteger();
      final AtomicInteger nodeLeftCount = new AtomicInteger();
      final AtomicInteger opsEnabledCount = new AtomicInteger();
      final AtomicInteger opsDisabledCount = new AtomicInteger();

      info.addClusterListener(new ClusterListener() {

        @Override
        public void onClusterEvent(ClusterEvent event) {
          debug("++++++ Got event: " + event.getType() + ", node: " + event.getNode().getId() + ", currentNode: "
                + info.getCurrentNode().getId());
          switch (event.getType()) {
            case NODE_JOINED:
              nodeJoinedCount.incrementAndGet();
              break;
            case NODE_LEFT:
              nodeLeftCount.incrementAndGet();
              break;
            case OPERATIONS_ENABLED:
              opsEnabledCount.incrementAndGet();
              break;
            case OPERATIONS_DISABLED:
              opsDisabledCount.incrementAndGet();
              break;
          }
        }
      });

      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          // attaching listener fires node join of current node
          int count = nodeJoinedCount.get();
          debug("Waiting till 1 node joins (current node itlself): got: " + count);
          return count == 1;
        }
      });

      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          // attaching listener fires ops enabled of current node
          int count = opsEnabledCount.get();
          debug("Waiting till 1 node ops enabled (current node itlself): got: " + count);
          return count == 1;
        }
      });

      Assert.assertEquals(0, nodeLeftCount.get());
      Assert.assertEquals(0, opsDisabledCount.get());

      int index = waitForAllClients();

      debug("My index: " + index + ", index-0 client Crashing active and waiting for passive to take over");
      if (index == 0) {
        getTestControlMbean().crashActiveAndWaitForPassiveToTakeOver(0);
      }

      // all clients should reconnect, ops disabled then enabled (fired only locally)
      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          int count = opsDisabledCount.get();
          debug("Waiting till ops disabled: got: " + count);
          return count == 1;
        }
      });

      CallableWaiter.waitOnCallable(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          int count = opsEnabledCount.get();
          debug("Waiting till ops enabled: got: " + count);
          return count == 2;
        }
      });

      debug("My index: " + index + ", All clients leaving except for index-0");
      if (index == 0) {
        CallableWaiter.waitOnCallable(new Callable<Boolean>() {
          @Override
          public Boolean call() throws Exception {
            int count = nodeLeftCount.get();
            debug("Waiting till 2 nodes leaves: got: " + count);
            return count == 2;
          }
        });
      }
    }
  }
}
