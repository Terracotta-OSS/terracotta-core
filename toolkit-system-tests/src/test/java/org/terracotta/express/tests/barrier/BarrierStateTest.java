/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.barrier;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

public class BarrierStateTest extends AbstractToolkitTestBase {

  public BarrierStateTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(BarrierStateClient.class, 3);
  }

  public static class BarrierStateClient extends ClientBase {

    public BarrierStateClient(String[] args) {
      super(args);
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      for (int i = 0; i < 1000; i++) {
        System.out.println("Reached" + i);
        toolkit.getBarrier("barrier" + i, getParticipantCount()).await();
      }
    }

  }

}
