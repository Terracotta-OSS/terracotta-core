/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.concurrent.ToolkitBarrier;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

public class ToolkitListMultipleNodeRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitListMultipleNodeRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitListMultipleNodeRejoinTestClient.class, ToolkitListMultipleNodeRejoinTestClient.class);
  }

  public static class ToolkitListMultipleNodeRejoinTestClient extends AbstractToolkitRejoinTestClient {
    private static final int              No_OF_PUTS                = 100;
    private final StringKeyValueGenerator keyValGr                  = new StringKeyValueGenerator();
    private final static int              START_INDEX_BEFORE_REJOIN = 0;
    private final static int              START_INDEX_AFTER_REJOIN  = 100;

    public ToolkitListMultipleNodeRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal toolkit = createRejoinToolkit();
      ToolkitList toolkitList = toolkit.getList("SomeList", String.class);
      ToolkitBarrier barrier = toolkit.getBarrier("SomeBarrier", 2);

      int clientIndex = barrier.await();

      if (clientIndex == 0) {
        doDebug("Client 0 trying to Put values..");
        doSomePuts(toolkitList, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);
      } else {
        doDebug("waiting while Client 0 trying to Put values");
      }
      doDebug("done");
      barrier.await();

      assertAllKeyValuePairsExist(toolkitList, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);
      barrier.await();
      doDebug("Starting rejoin...");

      if (clientIndex == 0) {
        startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);
      } else {
        waitUntilRejoinCompleted();
      }
      doDebug("Came back to app code after rejoin completed");
      barrier.await();
      assertAllKeyValuePairsExist(toolkitList, No_OF_PUTS, START_INDEX_BEFORE_REJOIN);

      if (clientIndex == 1) {
        doSomePuts(toolkitList, No_OF_PUTS, START_INDEX_AFTER_REJOIN);
      }
      assertAllKeyValuePairsExist(toolkitList, No_OF_PUTS, START_INDEX_AFTER_REJOIN);

    }

    private boolean assertAllKeyValuePairsExist(ToolkitList toolkitList, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        if (!toolkitList.contains(keyValGr.getValue(i))) {
          doDebug("toolkitList.contains(" + keyValGr.getValue(i) + ") = " + toolkitList.contains(keyValGr.getValue(i)));
          return false;
        }
      }
      return true;
    }

    private void doSomePuts(ToolkitList toolkitList, int noOfPuts, int startIndex) {
      for (int i = startIndex; i < noOfPuts; i++) {
        toolkitList.add(keyValGr.getValue(i));
        doDebug("value =" + keyValGr.getValue(i));
      }
    }

  }

}
