/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import junit.framework.Assert;

public class ToolkitSetRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitSetRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSetRejoinTestClient.class);
  }

  public static class ToolkitSetRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int              NUM_ELEMENTS = 10;
    private final StringKeyValueGenerator keyValGr     = new StringKeyValueGenerator();

    public ToolkitSetRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      doDebug("Creating ToolkitSet");
      ToolkitSet toolkitSet = tk.getSet("ToolkitSet", String.class);

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        toolkitSet.add(keyValGr.getValue(i));
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSet.contains(keyValGr.getValue(i)));
      }

      startRejoinAndWaitUnilRejoinHappened(testHandlerMBean, tk);

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
  }

}
