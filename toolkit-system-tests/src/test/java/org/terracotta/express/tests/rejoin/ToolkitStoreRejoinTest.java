/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.store.ToolkitStore;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

import junit.framework.Assert;

public class ToolkitStoreRejoinTest extends AbstractToolkitRejoinTest {

  public ToolkitStoreRejoinTest(TestConfig testConfig) {
    super(testConfig, ToolkitStoreRejoinTestClient.class);
  }

  public static class ToolkitStoreRejoinTestClient extends AbstractToolkitRejoinTestClient {

    private static final int NUM_ELEMENTS = 10;

    public ToolkitStoreRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Exception {
      ToolkitInternal tk = createRejoinToolkit();

      doDebug("Adding values to list before rejoin");
      final ToolkitStore<String, String> store = tk.getStore("someList", null);
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        store.put("key-" + i, "value-" + i);
      }

      doDebug("Asserting values before rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final String key = "key-" + i;
        final String actual = store.get(key);
        final String expected = "value-" + i;
        doDebug(" expected: " + expected + ", key: " + key + ", actual: " + actual);
        Assert.assertEquals(expected, actual);
      }

      startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);

      doDebug("Asserting old values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        Assert.assertEquals("value-" + i, store.get("key-" + i));
      }

      doDebug("Adding new values after rejoin");
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        store.put("key-after-rejoin-" + i, "value-after-rejoin-" + i);
      }

      for (String key : store.keySet()) {
        doDebug("Store content: Key: " + key + ", value: " + store.get(key));
      }

      doDebug("Asserting new values inserted after rejoin");
      Assert.assertEquals(2 * NUM_ELEMENTS, store.size());
      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final String key = "key-" + i;
        final String expected = "value-" + i;
        Assert.assertEquals(expected, store.get(key));
      }

      for (int i = 0; i < NUM_ELEMENTS; i++) {
        final String key = "key-after-rejoin-" + i;
        final String expected = "value-after-rejoin-" + i;
        Assert.assertEquals(expected, store.get(key));
      }

    }
  }

}
