/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterEvent.Type;
import org.terracotta.toolkit.collections.ToolkitSortedSet;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.Callable;

import junit.framework.Assert;

public class ToolkitSortedSetRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitSortedSetRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSortedSetRejoinTestClient.class);
  }

  public static class ToolkitSortedSetRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int              NUM_ELEMENTS = 10;
    private final StringKeyValueGenerator keyValGr     = new StringKeyValueGenerator();

    public ToolkitSortedSetRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      Toolkit tk = createRejoinToolkit();

      doDebug("Creating ToolkitSet");
      ToolkitSortedSet toolkitSortedSet = tk.getSortedSet("ToolkitSet", String.class);

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        toolkitSortedSet.add(keyValGr.getValue(i));
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSortedSet.contains(keyValGr.getValue(i)));
      }

      String msg = "";
      msg = "Crashing first active...";
      doDebug(msg);

      testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);

      msg = "Passive must have taken over as ACTIVE";
      doDebug(msg);

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          doDebug("Processing received events (waiting till rejoin happens for node: " + beforeRejoinNode + ")");
          for (ClusterEvent e : receivedEvents) {
            if (e.getType() == Type.NODE_REJOINED) {
              doDebug("Rejoin event - oldNode: " + e.getNode());
              if (e.getNode().getId().equals(beforeRejoinNode.getId())) {
                doDebug("Rejoin received for expected node - " + beforeRejoinNode);
                return true;
              }
            }
          }
          return false;
        }
      });

      doDebug("Rejoin happened successfully");
      doDebug("Asserting old values after rejoin");

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSortedSet.contains(keyValGr.getValue(i)));
      }

      doSleep(5);

      doDebug("Adding new values after rejoin");
      for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
        toolkitSortedSet.add(keyValGr.getValue(i));
      }

      doSleep(5);

      for (int i = 0; i < toolkitSortedSet.size(); i++) {
        doDebug("Got value for i: " + i + ", value: "
                + (toolkitSortedSet.contains(keyValGr.getValue(i)) ? keyValGr.getValue(i) : null));
      }
      doSleep(10);

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, toolkitSortedSet.size());
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSortedSet.contains(keyValGr.getValue(i)));
      }
      doDebug("Asserted new values");

      doSleep(10);

  }
  }
}
