/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.LiteralKeyLiteralValueGenerator;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

public class ToolkitSortedMapMultipleNodeRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitSortedMapMultipleNodeRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitMapMultipleNodeRejoinTestClient.class, ToolkitMapMultipleNodeRejoinTestClient.class);
    testConfig.setRestartable(false);
  }

  public static class ToolkitMapMultipleNodeRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static final int NUM_ELEMENTS              = 1000;
    private final static int              START_INDEX_BEFORE_REJOIN = 0;
    private final static int              START_INDEX_AFTER_REJOIN  = 100;

    public ToolkitMapMultipleNodeRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      keyValueGenerator = new LiteralKeyLiteralValueGenerator();
      ToolkitSortedMap toolkitMap = toolkit.getSortedMap("SomeSortedMap", String.class, String.class);
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", 2);

      int clientIndex = barrier.await();

      if (clientIndex == 0) {
        doDebug("Client 0 trying to Put values in map..");
        doSomePuts(toolkitMap, NUM_ELEMENTS, START_INDEX_BEFORE_REJOIN);
      } else {
        doDebug("waiting while Client 0 trying to Put values in map");
      }
      doDebug("done");
      barrier.await();

      assertAllKeyValuePairsExist(toolkitMap, NUM_ELEMENTS, START_INDEX_BEFORE_REJOIN);
      barrier.await();
      doDebug("Starting rejoin...");

      if (clientIndex == 0) {
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        waitUntilRejoinCompleted();
      }
      doDebug("Came back to app code after rejoin completed");
      barrier.await();
      assertAllKeyValuePairsExist(toolkitMap, NUM_ELEMENTS, START_INDEX_BEFORE_REJOIN);

      if (clientIndex == 1) {
        doSomePuts(toolkitMap, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);
      }
      assertAllKeyValuePairsExist(toolkitMap, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);

      barrier.await();
      ToolkitSortedMap freshMap = toolkit.getSortedMap("freshSortedMap", String.class, String.class);
      if (clientIndex == 0) {
        doSomePuts(freshMap, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);
      }
      barrier.await();
      assertAllKeyValuePairsExist(freshMap, NUM_ELEMENTS, START_INDEX_AFTER_REJOIN);
    }

    private boolean assertAllKeyValuePairsExist(ToolkitSortedMap toolkitMap, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        String value = (String) toolkitMap.get(getKey(i));
        if (!value.equals(getValue(i))) {
          doDebug("found - value: " + value + "for key: " + getKey(i));
          return false;
        }
      }
      return true;
    }

    private void doSomePuts(ToolkitSortedMap toolkitMap, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        toolkitMap.put(getKey(i), getValue(i));
        doDebug("putting: key- " + getKey(i) + "value -" + getValue(i));
      }
    }

    private String getValue(int i) {
      return (String) keyValueGenerator.getValue(i);
    }

    private String getKey(int i) {
      return (String) keyValueGenerator.getKey(i);
    }

  }

}
