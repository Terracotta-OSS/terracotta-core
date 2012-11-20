/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.cluster.RejoinClusterEvent;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class RejoinTest extends AbstractToolkitRejoinTest {

  public RejoinTest(TestConfig testConfig) {
    super(testConfig, RejoinTestClient.class);
  }

  public static class RejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int NUM_ELEMENTS = 10;

    public RejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Exception {
      debug("Creating first toolkit");
      Properties properties = new Properties();
      properties.put("rejoin", "true");
      Toolkit tk = ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl(), properties);
      final List<ClusterEvent> receivedEvents = new ArrayList<ClusterEvent>();
      final ClusterNode beforeRejoinNode = tk.getClusterInfo().getCurrentNode();

      tk.getClusterInfo().addClusterListener(new ClusterListener() {

        @Override
        public void onClusterEvent(ClusterEvent event) {
          debug("Received cluster event: " + event);
          receivedEvents.add(event);
        }
      });

      ToolkitLogger logger = ((ToolkitInternal) tk).getLogger("com.tc.AppLogger");

      debug("Adding values to list before rejoin");
      ToolkitList<String> list = tk.getList("someList", null);
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        list.add("value-" + i);
      }

      debug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, list.get(i));
      }

      String msg = "";
      msg = "Crashing first active...";
      debug(msg);
      logger.info(msg);

      // testHandlerMBean.crashActiveServer(0);
      //
      // msg = "Crashed active server";
      // debug(msg);

      testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);

      msg = "Passive must have taken over as ACTIVE";
      debug(msg);
      logger.info(msg);

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          debug("Processing received events (waiting till rejoin happens for node: " + beforeRejoinNode + ")");
          for (ClusterEvent e : receivedEvents) {
            if (e instanceof RejoinClusterEvent) {
              RejoinClusterEvent re = (RejoinClusterEvent) e;
              debug("Rejoin event - oldNode: " + re.getNodeBeforeRejoin() + ", newNode: " + re.getNodeAfterRejoin());
              if (re.getNodeBeforeRejoin().getId().equals(beforeRejoinNode.getId())) {
                debug("Rejoin received for expected node - " + beforeRejoinNode);
                return true;
              }
            }
          }
          return false;
        }
      });

      debug("Rejoin happened successfully");
      debug("Asserting old values after rejoin");

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, list.get(i));
      }

      debug("Adding new values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        list.add("value-after-rejoin-" + (i + NUM_ELEMENTS));
      }

      for (int i = 0; i < list.size(); i++) {
        debug("Got value for i: " + i + ", value: " + list.get(i));
      }

      debug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, list.size());
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        final String expected;
        if (i < NUM_ELEMENTS) {
          expected = "value-" + i;
        } else {
          expected = "value-after-rejoin-" + i;
        }
        Assert.assertEquals(expected, list.get(i));
      }

    }

  }

}
