/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.LiteralKeyLiteralValueGenerator;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;
import com.tc.util.Assert;

import java.util.Map;

public class ToolkitMapAndSortedMapRejoinTest extends AbstractToolkitRejoinTest {
  public ToolkitMapAndSortedMapRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitMapAndSortedMapRejoinTestClient.class);
  }

  public static class ToolkitMapAndSortedMapRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int              NUM_ELEMENTS = 1000;
    private ToolkitInternal               toolkit;

    public ToolkitMapAndSortedMapRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      toolkit = createRejoinToolkit();
      keyValueGenerator = new LiteralKeyLiteralValueGenerator();
      ToolkitMap toolkitMap = toolkit.getMap("rejoinTestMap", String.class, String.class);
      doDebug("Running Test For Map");
      runTestFor(toolkitMap, testHandlerMBean);
      receivedEvents.clear();
      doDebug("Done Testing Map, Now testing SortedMap");

      testHandlerMBean.restartLastCrashedServer(0);

      ToolkitSortedMap toolkitSortedMap = toolkit.getSortedMap("rejoinTestSortedMap", String.class, String.class);
      doDebug("Running test for SortedMap");
      runTestFor(toolkitSortedMap, testHandlerMBean);
      receivedEvents.clear();
      doDebug("Done Testing SortedMap, Exiting!");
    }

    private String getKey(int i) {
      return (String) keyValueGenerator.getKey(i);
    }

    private String getValue(int i) {
      return (String) keyValueGenerator.getValue(i);
    }
    
    private void runTestFor(Map toolkitMap, TestHandlerMBean testHandlerMBean) throws Exception {

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        toolkitMap.put(getKey(i), getValue(i));
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals(toolkitMap.get(getKey(i)), getValue(i));
      }

      startRejoinAndWaitUntilCompleted(testHandlerMBean, toolkit);

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals(toolkitMap.get(getKey(i)), getValue(i));
      }


      doDebug("Adding new values after rejoin");
      for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
        toolkitMap.put(getKey(i), getValue(i));
      }


      for (int i = 0; i < toolkitMap.size(); i++) {
        doDebug("Got value for i: " + i + ", value: " + (toolkitMap.get(getValue(i))));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, toolkitMap.size());
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        Assert.assertEquals(toolkitMap.get(getKey(i)), getValue(i));
      }
      doDebug("Asserted new values");


    }

  }
}