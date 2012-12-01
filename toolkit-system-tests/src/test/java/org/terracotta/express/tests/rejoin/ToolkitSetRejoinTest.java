/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.LiteralKeyLiteralValueGenerator;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import java.util.concurrent.BrokenBarrierException;

import junit.framework.Assert;

public class ToolkitSetRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitSetRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSetRejoinTestClient.class, ToolkitSetRejoinTestClient.class);
    testConfig.setRestartable(false);
  }

  public static class ToolkitSetRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static final int              No_OF_PUTS                = 100;
    private final static int              START_INDEX_BEFORE_REJOIN = 0;
    private final static int              START_INDEX_AFTER_REJOIN  = 100;

    public ToolkitSetRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      keyValueGenerator = new LiteralKeyLiteralValueGenerator();
      ToolkitSet toolkitSet = toolkit.getSet("SomeSet", String.class);
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", 2);

      int clientIndex = barrier.await();

      if (clientIndex == 0) {
        doDebug("Running test for single Node");
        testForSingleNode(toolkitSet, testHandlerMBean, toolkit);
        doDebug("Client 0 is out of Single node test..");
      } else {
        doDebug("Client 0 is executing the single Node Test, i'm supposed to wait...");
        waitUntilRejoinCompleted();// wait until client 0 is done with rejoin while running the single node rejoin test
        doDebug("In node 0 rejoin has occured while running test for single node");
      }
      barrier.await();

      testForMultipleNodes(testHandlerMBean, toolkit, toolkitSet, barrier, clientIndex);

      testForFreshDsAfterRejoin(testHandlerMBean, toolkit, toolkitSet, barrier, clientIndex);
    }

    private void testForFreshDsAfterRejoin(TestHandlerMBean testHandlerMBean, ToolkitInternal toolkit,
                                           ToolkitSet toolkitSet, ToolkitBarrier barrier, int clientIndex)
        throws InterruptedException, BrokenBarrierException {
      toolkitSet = toolkit.getSet("freshSet", String.class);
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        doSomePuts(toolkitSet, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);
      }
      barrier.await();

    }

    private void testForMultipleNodes(TestHandlerMBean testHandlerMBean, ToolkitInternal toolkit,
                                      ToolkitSet toolkitSet, ToolkitBarrier barrier, int clientIndex)
        throws InterruptedException, BrokenBarrierException, Exception {
      clientIndex = barrier.await();
      if (clientIndex == 0) {
        doSomePuts(toolkitSet, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);
      }
      barrier.await();
      assertAllKeyValuePairsExist(toolkitSet, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);
      barrier.await();
      doDebug("Starting rejoin...");

      if (clientIndex == 0) {
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        waitUntilRejoinCompleted();
      }
      doDebug("Came back to app code after rejoin completed");
      barrier.await();
      assertAllKeyValuePairsExist(toolkitSet, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);

      if (clientIndex == 1) {
        doSomePuts(toolkitSet, No_OF_PUTS, START_INDEX_AFTER_REJOIN);
      }
      assertAllKeyValuePairsExist(toolkitSet, No_OF_PUTS, START_INDEX_AFTER_REJOIN);
      barrier.await();

      doDebug("clearing Set..");
      if (clientIndex == 0) {
        toolkitSet.clear();
      }
      barrier.await();

      toolkitSet = toolkit.getSet("FreshSet", String.class);
      if (clientIndex == 0) {
        doDebug("Client 0 is adding values to fresh Set");
        doSomePuts(toolkitSet, No_OF_PUTS, START_INDEX_AFTER_REJOIN);
      } else {
        doDebug("Client 0 is adding values to fresh Set, i'm suppossed to wait..");
      }
      barrier.await();
      doDebug("Asserting that all values put by client 0 are present");
      assertAllKeyValuePairsExist(toolkitSet, No_OF_PUTS, START_INDEX_AFTER_REJOIN);

    }

    private void testForSingleNode(ToolkitSet toolkitSet, TestHandlerMBean testHandlerMBean, ToolkitInternal toolkit)
        throws Exception {

      for (int i = 0; i < No_OF_PUTS; i++) {
        toolkitSet.add(getValue(i));
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < No_OF_PUTS; i++) {
        Assert.assertTrue(toolkitSet.contains(getValue(i)));
      }

      startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < No_OF_PUTS; i++) {
        Assert.assertTrue(toolkitSet.contains(getValue(i)));
      }

      doDebug("Adding new values after rejoin");
      for (int i = No_OF_PUTS; i < 2 * No_OF_PUTS; i++) {
        toolkitSet.add(getValue(i));
      }

      for (int i = 0; i < toolkitSet.size(); i++) {
        doDebug("Got value for i: " + i + ", value: "
 + (toolkitSet.contains(getValue(i)) ? getValue(i) : null));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * No_OF_PUTS, toolkitSet.size());
      for (int i = 0; i < 2 * No_OF_PUTS; i++) {
        Assert.assertTrue(toolkitSet.contains(getValue(i)));
      }
      doDebug("Asserted new values");
      doDebug("Single Node Test Passed");
      cleanUp(toolkitSet, testHandlerMBean);
    }

    private void cleanUp(ToolkitSet toolkitSet, TestHandlerMBean testHandlerMBean) throws Exception {
      doDebug("Clearing Set And Starting MultiNode Test");
      toolkitSet.clear();
      doDebug("Set cleared, starting the crashed server");
      testHandlerMBean.restartLastCrashedServer(0);
    }

    private boolean assertAllKeyValuePairsExist(ToolkitSet toolkitSet, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        if (!toolkitSet.contains(getValue(i))) {
          doDebug("toolkitSet.contains(" + getValue(i) + ") = " + toolkitSet.contains(getValue(i)));
          return false;
        }
      }
      return true;
    }

    private void doSomePuts(ToolkitSet toolkitSet, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        toolkitSet.add(getValue(i));
        doDebug("value =" + getValue(i));
      }
    }

    private String getValue(int i) {
      return (String) keyValueGenerator.getValue(i);
    }

  }

}
