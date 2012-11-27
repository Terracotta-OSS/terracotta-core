/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

public class ToolkitSetMultipleNodeRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitSetMultipleNodeRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSetMultipleNodeRejoinTestClient.class, ToolkitSetMultipleNodeRejoinTestClient.class);
  }

  public static class ToolkitSetMultipleNodeRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static final int              No_OF_PUTS                = 100;
    private final StringKeyValueGenerator keyValGr                  = new StringKeyValueGenerator();
    private final static int              START_INDEX_BEFORE_REJOIN = 0;
    private final static int              START_INDEX_AFTER_REJOIN  = 100;

    public ToolkitSetMultipleNodeRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      ToolkitSet toolkitSet = toolkit.getSet("SomeSet", String.class);
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", 2);

      int clientIndex = barrier.await();

      if (clientIndex == 0) {
        doDebug("Client 0 trying to Put values..");
        doSomePuts(toolkitSet, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);
      } else {
        doDebug("waiting while Client 0 trying to Put values");
      }
      doDebug("done");
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

    }

    private boolean assertAllKeyValuePairsExist(ToolkitSet toolkitSet, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        if (!toolkitSet.contains(keyValGr.getValue(i))) {
          doDebug("toolkitSet.contains(" + keyValGr.getValue(i) + ") = " + toolkitSet.contains(keyValGr.getValue(i)));
          return false;
        }
      }
      return true;
    }

    private void doSomePuts(ToolkitSet toolkitSet, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        toolkitSet.add(keyValGr.getValue(i));
        doDebug("value =" + keyValGr.getValue(i));
      }
    }

  }

}
