/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.LiteralKeyLiteralValueGenerator;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

public class ToolkitSortedSetMultipleNodeRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitSortedSetMultipleNodeRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSortedSetMultipleNodeRejoinTestClient.class,
          ToolkitSortedSetMultipleNodeRejoinTestClient.class);
    testConfig.getL2Config().setRestartable(false);
  }

  public static class ToolkitSortedSetMultipleNodeRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static final int NUM_ELEMENTS              = 1000;
    private final static int              START_INDEX_BEFORE_REJOIN = 0;
    private final static int              START_INDEX_AFTER_REJOIN  = 1000;

    public ToolkitSortedSetMultipleNodeRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      keyValueGenerator = new LiteralKeyLiteralValueGenerator();
      ToolkitSortedSet toolkitSortedSet = toolkit.getSortedSet("SomeSortedSet", String.class);
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", 2);

      int clientIndex = barrier.await();

      if (clientIndex == 0) {
        doDebug("Client 0 trying to Put values..");
        doSomePuts(toolkitSortedSet, NUM_ELEMENTS, START_INDEX_BEFORE_REJOIN);
      } else {
        doDebug("waiting while Client 0 trying to Put values");
      }
      doDebug("done");
      barrier.await();

      assertAllKeyValuePairsExist(toolkitSortedSet, NUM_ELEMENTS, START_INDEX_BEFORE_REJOIN);
      barrier.await();
      doDebug("Starting rejoin...");

      if (clientIndex == 0) {
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        waitUntilRejoinCompleted();
      }
      doDebug("Came back to app code after rejoin completed");
      barrier.await();
      assertAllKeyValuePairsExist(toolkitSortedSet, NUM_ELEMENTS, START_INDEX_BEFORE_REJOIN);

      if (clientIndex == 1) {
        doSomePuts(toolkitSortedSet, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);
      }
      assertAllKeyValuePairsExist(toolkitSortedSet, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);

      barrier.await();

      ToolkitSortedSet freshSortedSet = toolkit.getSortedSet("freshSortedSet", String.class);
      if (clientIndex == 0) {
        doSomePuts(freshSortedSet, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);
      }
      barrier.await();
      assertAllKeyValuePairsExist(freshSortedSet, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);
    }

    private boolean assertAllKeyValuePairsExist(ToolkitSortedSet toolkitSortedSet, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        if (!toolkitSortedSet.contains(getValue(i))) {
          doDebug("toolkitSortedSet.contains(" + getValue(i) + ") = " + toolkitSortedSet.contains(getValue(i)));
          return false;
        }
      }
      return true;
    }

    private void doSomePuts(ToolkitSortedSet toolkitSortedSet, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        toolkitSortedSet.add(getValue(i));
        doDebug("value =" + getValue(i));
      }
    }

    private String getValue(int i) {
      return (String) keyValueGenerator.getValue(i);
    }

  }

}
