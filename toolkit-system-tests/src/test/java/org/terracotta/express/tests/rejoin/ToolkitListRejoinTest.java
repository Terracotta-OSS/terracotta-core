/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.collections.ToolkitBlockingQueue;
import org.terracotta.toolkit.collections.ToolkitList;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import junit.framework.Assert;

public class ToolkitListRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitListRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitListRejoinTestClient.class);
  }

  public static class ToolkitListRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int NUM_ELEMENTS = 10;

    public ToolkitListRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Exception {
      ToolkitInternal tk = createRejoinToolkit();

      doDebug("Adding values to list before rejoin");
      ToolkitList<String> list = tk.getList("someList", null);
      ToolkitBlockingQueue<String> queue = tk.getBlockingQueue("someTBQ", null);
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        list.add("value-" + i);
        queue.add("value-" + i);
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, list.get(i));
        Assert.assertTrue(queue.contains("value-" + i));
      }

      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
      doDebug("Asserting old values after rejoin");

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, list.get(i));
        Assert.assertTrue(queue.contains("value-" + i));
      }


      doDebug("Adding new values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        list.add("value-after-rejoin-" + (i + NUM_ELEMENTS));
        queue.add("value-after-rejoin-" + (i + NUM_ELEMENTS));
      }


      for (int i = 0; i < list.size(); i++) {
        doDebug("Got value for i: " + i + ", value: " + list.get(i));
        doDebug("Got value for i: " + i + ", value: " + (queue.contains("value-" + i) ? "value-" + i : null));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, list.size());
      Assert.assertEquals(2 * NUM_ELEMENTS, queue.size());
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        final String expected;
        if (i < NUM_ELEMENTS) {
          expected = "value-" + i;
        } else {
          expected = "value-after-rejoin-" + i;
        }
        Assert.assertEquals(expected, list.get(i));
        Assert.assertTrue(queue.contains(expected));
      }
      doDebug("Asserted new values");


    }

  }

}
