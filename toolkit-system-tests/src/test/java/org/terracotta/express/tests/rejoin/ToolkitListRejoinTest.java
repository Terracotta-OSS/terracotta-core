/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.NonLiteralKeyNonLiteralValueGenerator;
import org.terracotta.express.tests.util.TCInt;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import junit.framework.Assert;

public class ToolkitListRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitListRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitListRejoinTestClient.class, ToolkitListRejoinTestClient.class);
    testConfig.getL2Config().setRestartable(false);
  }

  public static class ToolkitListRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int NUM_ELEMENTS = 10;

    public ToolkitListRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Exception {
      ToolkitInternal tk = createRejoinToolkit();
      keyValueGenerator = new NonLiteralKeyNonLiteralValueGenerator();
      ToolkitBarrier toolkitListBarrier = tk.getBarrier("toolkitListBarrier", 2);

      int index = toolkitListBarrier.await();
      final ToolkitList<TCInt> toolkitList = tk.getList("toolkitList", TCInt.class);
      doDebug("client " + index + " starting.. ");
      if (index == 0) {
        doDebug("Adding values before rejoin");
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          toolkitList.add((TCInt) keyValueGenerator.getValue(i));
        }
        startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
      } else {
        waitUntilRejoinCompleted();
      }
      debug("client " + index + " done with rejoin");
      toolkitListBarrier.await();

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitList.contains(keyValueGenerator.getValue(i)));
      }
      toolkitListBarrier.await();

      if (index == 1) {
        doDebug("Adding new values after rejoin");
        for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
          toolkitList.add((TCInt) keyValueGenerator.getValue(i));
        }
      }
      toolkitListBarrier.await();

      doDebug("Asserting new values after rejoin");
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitList.contains(keyValueGenerator.getValue(i)));
      }

      doDebug("getting a fresh list after rejoin");
      ToolkitList<TCInt> freshToolkitList = tk.getList("freshToolkitList", TCInt.class);
      if (index == 1) {
        doDebug("adding values in fresh blocking queue after rejoin");
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          freshToolkitList.add((TCInt) keyValueGenerator.getValue(i));
        }
        tk.waitUntilAllTransactionsComplete();
        doDebug("asserting fresh blocking queue after rejoin " + freshToolkitList.size());
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          Assert.assertTrue(freshToolkitList.contains(keyValueGenerator.getValue(i)));
        }
        toolkitListBarrier.await();
      } else {
        toolkitListBarrier.await();
        doDebug("asserting fresh blocking queue after rejoin " + freshToolkitList.size());
        for (int i = 0; i < NUM_ELEMENTS; i++) {
          Assert.assertTrue(freshToolkitList.contains(keyValueGenerator.getValue(i)));
        }
      }
    }

  }

}
