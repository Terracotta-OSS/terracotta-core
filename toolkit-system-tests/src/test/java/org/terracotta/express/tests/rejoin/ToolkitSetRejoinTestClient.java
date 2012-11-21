/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.rejoin.AbstractToolkitRejoinTest.AbstractToolkitRejoinTestClient;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.cluster.ClusterEvent;
import org.terracotta.toolkit.cluster.ClusterListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.cluster.RejoinClusterEvent;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;

import com.tc.test.jmx.TestHandlerMBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class ToolkitSetRejoinTestClient extends AbstractToolkitRejoinTestClient {

  private static final int              NUM_ELEMENTS = 10;
  private ToolkitLogger                 logger;
  private final StringKeyValueGenerator keyValGr     = new StringKeyValueGenerator();

  public ToolkitSetRejoinTestClient(String[] args) {
    super(args);
  }

  @Override
  protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
    debug("Creating first toolkit");
    Properties properties = new Properties();
    properties.put("rejoin", "true");
    Toolkit tk = ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl(), properties);
    final List<ClusterEvent> receivedEvents = new ArrayList<ClusterEvent>();
    final ClusterNode beforeRejoinNode = tk.getClusterInfo().getCurrentNode();
    logger = ((ToolkitInternal) tk).getLogger("com.tc.AppLogger");
    tk.getClusterInfo().addClusterListener(new ClusterListener() {

      @Override
      public void onClusterEvent(ClusterEvent event) {
        doDebug("Received cluster event: " + event);
        receivedEvents.add(event);
      }
    });
    doDebug("Creating ToolkitSet");
    ToolkitSet toolkitSet = tk.getSet("ToolkitSet", String.class);

    for (int i = 0; i < NUM_ELEMENTS; i++) {
      toolkitSet.add(keyValGr.getValue(i));
    }

    doDebug("Asserting values before rejoin");
    for (int i = 0; i < NUM_ELEMENTS; i++) {
      Assert.assertTrue(toolkitSet.contains(keyValGr.getValue(i)));
    }

    String msg = "";
    msg = "Crashing first active...";
    doDebug(msg);
    logger.info(msg);
    // testHandlerMBean.crashActiveServer(0);
    //
    // msg = "Crashed active server";
    // doDebug(msg);

    testHandlerMBean.crashActiveAndWaitForPassiveToTakeOver(0);

    msg = "Passive must have taken over as ACTIVE";
    doDebug(msg);
    logger.info(msg);

    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

      @Override
      public Boolean call() throws Exception {
        doDebug("Processing received events (waiting till rejoin happens for node: " + beforeRejoinNode + ")");
        for (ClusterEvent e : receivedEvents) {
          if (e instanceof RejoinClusterEvent) {
            RejoinClusterEvent re = (RejoinClusterEvent) e;
            doDebug("Rejoin event - oldNode: " + re.getNodeBeforeRejoin() + ", newNode: " + re.getNodeAfterRejoin());
            if (re.getNodeBeforeRejoin().getId().equals(beforeRejoinNode.getId())) {
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
      Assert.assertTrue(toolkitSet.contains(keyValGr.getValue(i)));
    }

    doSleep(5);

    doDebug("Adding new values after rejoin");
    for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
      toolkitSet.add(keyValGr.getValue(i));
    }

    doSleep(5);

    for (int i = 0; i < toolkitSet.size(); i++) {
      doDebug("Got value for i: " + i + ", value: "
              + (toolkitSet.contains(keyValGr.getValue(i)) ? keyValGr.getValue(i) : null));
    }
    doSleep(10);

    doDebug("Asserting new values inserted after rejoin");
    Assert.assertEquals(2 * NUM_ELEMENTS, toolkitSet.size());
    for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
      Assert.assertTrue(toolkitSet.contains(keyValGr.getValue(i)));
    }
    doDebug("Asserted new values");

    doSleep(10);

  }

  private void doDebug(String string) {
    debug(string);
    if (logger != null) {
      logger.info("___XXX___: " + string);
    }
  }

  private void doSleep(int sec) throws InterruptedException {
    for (int i = 0; i < sec; i++) {
      doDebug("Sleeping for 1 sec (" + i + "/" + sec + ")");
      Thread.sleep(1000);
    }
  }
}
