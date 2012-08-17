/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests.statistics;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.toolkit.Toolkit;

import com.tc.properties.TCPropertiesConsts;
import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.atomic.AtomicBoolean;

public class IllegalReconnectTest extends AbstractToolkitTestBase {

  public IllegalReconnectTest(TestConfig testConfig) {
    super(testConfig, App.class);
    testConfig.getL2Config().setProxyDsoPorts(true);
    testConfig.addTcProperty(TCPropertiesConsts.L2_L1RECONNECT_ENABLED, "false");
    testConfig.getL2Config().setProxyWaitTime(Integer.MAX_VALUE);
  }

  @Override
  protected void startClients() throws Throwable {
    final AtomicBoolean passed = new AtomicBoolean(true);
    Thread th = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          runClient(App.class);
        } catch (Throwable e) {
          e.printStackTrace();
          passed.set(false);
        }
      }
    });
    th.start();

    Thread.sleep(5000);
    testServerManager.closeClientConnections(0);
  }

  public static class App extends ClientBase {

    public App(String[] args) {
      super(args);
      assertEquals(1, getParticipantCount());
    }

    @Override
    protected void test(Toolkit toolkit) throws Throwable {
      ThreadUtil.reallySleep(20000);
    }

  }

}
