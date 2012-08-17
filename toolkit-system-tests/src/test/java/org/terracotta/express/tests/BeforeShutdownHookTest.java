/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFactory;
import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

public class BeforeShutdownHookTest extends AbstractToolkitTestBase {

  public BeforeShutdownHookTest(TestConfig testConfig) {
    super(testConfig, BeforeShutdownHookTestClient.class);
  }

  public static class BeforeShutdownHookTestClient extends ClientBase {

    public BeforeShutdownHookTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      String tcUrl = getTerracottaUrl();
      debug("Starting new client with tcUrl: " + tcUrl);
      Toolkit tk = ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl()
                                                + ",some_extra_params:1234");
      Assert.assertTrue("Toolkit should implement ToolkitInternal", tk instanceof ToolkitInternal);

      ToolkitInternal toolkitInternal = (ToolkitInternal) tk;
      final AtomicBoolean shutdownHookCalled = new AtomicBoolean(false);
      toolkitInternal.registerBeforeShutdownHook(new Runnable() {
        @Override
        public void run() {
          debug("In shutdown hook");
          shutdownHookCalled.set(true);
        }
      });

      debug("Shutting down new client...");
      tk.shutdown();
      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return shutdownHookCalled.get();
        }
      });
    }

  }
}
