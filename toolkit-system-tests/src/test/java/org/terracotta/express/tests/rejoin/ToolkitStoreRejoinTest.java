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
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class ToolkitStoreRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitStoreRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitStoreRejoinTestClient.class);
  }

  public static class ToolkitStoreRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int NUM_ELEMENTS = 10;
    private ToolkitLogger    logger;

    public ToolkitStoreRejoinTestClient(String[] args) {
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

      logger = ((ToolkitInternal) tk).getLogger("com.tc.AppLogger");

      doDebug("Adding values to list before rejoin");
      final ToolkitStore<String, String> store = tk.getStore("someList", null);
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        store.put("key-" + i, "value-" + i);
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final String key = "key-" + i;
        final String actual = store.get(key);
        final String expected = "value-" + i;
        doDebug(" expected: " + expected + ", key: " + key + ", actual: " + actual);
        Assert.assertEquals(expected, actual);
      }

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return store.size() == NUM_ELEMENTS;
        }
      });
      doDebug("Got size: " + store.size());
      ((ToolkitInternal) tk).waitUntilAllTransactionsComplete();

      doDebug("Crashing first active...");
      testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);

      doDebug("Passive must have taken over as ACTIVE");

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

      doDebug("Rejoin happened successfully");
      doDebug("Asserting old values after rejoin");

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, store.get("key-" + i));
      }

      doDebug("Adding new values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        store.put("key-after-rejoin-" + i, "value-after-rejoin-" + i);
      }

      for (String key : store.keySet()) {
        doDebug("Store content: Key: " + key + ", value: " + store.get(key));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, store.size());
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final String key = "key-" + i;
        final String expected = "value-" + i;
        Assert.assertEquals(expected, store.get(key));
      }

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final String key = "key-after-rejoin-" + i;
        final String expected = "value-after-rejoin-" + i;
        Assert.assertEquals(expected, store.get(key));
      }

    }

    private void doDebug(String string) {
      debug(string);
      logger.info("___XXXX___: " + string);
    }

  }

}
