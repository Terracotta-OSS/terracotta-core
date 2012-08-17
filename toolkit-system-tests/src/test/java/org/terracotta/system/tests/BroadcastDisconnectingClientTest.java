/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;

import com.tc.test.config.model.TestConfig;

public class BroadcastDisconnectingClientTest extends AbstractToolkitTestBase {

  private static final int LONG_RUNNING_CLIENT_COUNT  = 2;             // includes one that just
  private static final int LONG_RUNNERS_DURATION      = 10 * 60 * 1000;
  private static final int SHORT_RUNNERS_DURATION     = 30 * 1000;
  private static final int SHORT_RUNNERS_INTERVAL     = 30 * 1000;
  private static final int SHORT_RUNNING_CLIENT_COUNT = 8;

  public BroadcastDisconnectingClientTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().addExtraClientJvmArg("-DlongDuration=" + LONG_RUNNERS_DURATION);
    testConfig.getClientConfig().addExtraClientJvmArg("-DshortDuration=" + SHORT_RUNNERS_DURATION);
  }

  @Override
  protected void startClients() throws Throwable {
    Runner[] runners = new Runner[LONG_RUNNING_CLIENT_COUNT];
    for (int i = 0; i < LONG_RUNNING_CLIENT_COUNT; i++) {
      Runner runner = new Runner(BroadcastDisconnectingClientApp.class);
      runners[i] = runner;
      runner.start();
    }
    SmallClientRunner smallClientRunner = new SmallClientRunner();
    smallClientRunner.start();

    for (Runner runner : runners) {
      runner.finish();
    }

  }

  private class SmallClientRunner extends Thread {
    @Override
    public void run() {

      for (int i = 0; i < SHORT_RUNNING_CLIENT_COUNT; i++) {
        System.out.println("**** Going to spawn short client ");
        try {
          Thread.sleep(BroadcastDisconnectingClientTest.SHORT_RUNNERS_INTERVAL);
          runClient(DisconnectingALClient.class);
        } catch (Throwable e) {
          throw new AssertionError(e);
        }
      }

    }
  }

}
