/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.rejoin;

import org.terracotta.toolkit.internal.ToolkitInternal;

import com.tc.test.config.model.TestConfig;
import com.tc.test.jmx.TestHandlerMBean;

public class MultipleRejoinTest extends AbstractToolkitRejoinTest {

  public MultipleRejoinTest(TestConfig testConfig) {
    super(testConfig, MultipleRejoinTestClient.class);
  }

  public static class MultipleRejoinTestClient extends AbstractToolkitRejoinTestClient {

    public MultipleRejoinTestClient(String[] args) {
      super(args);
    }

    @Override
    protected void doRejoinTest(TestHandlerMBean testHandlerMBean) throws Exception {
      ToolkitInternal tk = createRejoinToolkit();

      int numRejoins = 0;

      while (numRejoins < 20) {
        doDebug("Number of rejoins till now: " + numRejoins + ", perm gen: " + getPermGenUsage());
        startRejoinAndWaitUntilCompleted(testHandlerMBean, tk);
        doDebug("Starting last crashed server...");
        testHandlerMBean.restartLastCrashedServer(0);
        doDebug("Waiting for passive to come up in cluster");
        testHandlerMBean.waitUntilPassiveStandBy(0);
        numRejoins++;
      }

    }

  }

}
