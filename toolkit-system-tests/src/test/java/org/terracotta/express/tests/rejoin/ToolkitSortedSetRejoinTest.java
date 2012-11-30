/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.express.tests.util.LiteralKeyLiteralValueGenerator;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import junit.framework.Assert;

public class ToolkitSortedSetRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitSortedSetRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitSortedSetRejoinTestClient.class);
  }

  public static class ToolkitSortedSetRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int              NUM_ELEMENTS = 10;

    public ToolkitSortedSetRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Throwable {
      ToolkitInternal tk = createRejoinToolkit();
      keyValueGenerator = new LiteralKeyLiteralValueGenerator();
      doDebug("Creating ToolkitSet");
      ToolkitSortedSet toolkitSortedSet = tk.getSortedSet("ToolkitSet", String.class);

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        toolkitSortedSet.add(getValue(i));
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSortedSet.contains(getValue(i)));
      }

      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSortedSet.contains(getValue(i)));
      }

      doDebug("Adding new values after rejoin");
      for (int i = NUM_ELEMENTS; i < 2 * NUM_ELEMENTS; i++) {
        toolkitSortedSet.add(getValue(i));
      }

      for (int i = 0; i < toolkitSortedSet.size(); i++) {
        doDebug("Got value for i: " + i + ", value: "
 + (toolkitSortedSet.contains(getValue(i)) ? getValue(i) : null));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, toolkitSortedSet.size());
      for (int i = 0; i < 2 * NUM_ELEMENTS; i++) {
        Assert.assertTrue(toolkitSortedSet.contains(getValue(i)));
      }
      doDebug("Asserted new values");
    }

    private String getValue(int i) {
      return (String) keyValueGenerator.getValue(i);
    }

  }
}
