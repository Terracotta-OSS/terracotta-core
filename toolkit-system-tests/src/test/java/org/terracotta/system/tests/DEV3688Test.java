/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.system.tests;

import org.terracotta.express.tests.base.AbstractExpressActivePassiveTest;
import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.test.config.model.PersistenceMode;
import com.tc.test.config.model.TestConfig;
import com.tc.util.concurrent.ThreadUtil;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

/**
 * Steps to reproduce DEV-3688:<br>
 * <br>
 * 1. Start A which becomes ACTIVE<br>
 * 2. Start a client which creates root and does something<br>
 * 3. Stop A<br>
 * 4. Start A again which will again start as ACTIVE<br>
 * 5. Start B which will become PASSIVE<br>
 * 6. Stop A so B becomes ACTIVE<br>
 * 7. Start a new client now and you will see that the server has again given ObjectID=1000<br>
 */
public class DEV3688Test extends AbstractExpressActivePassiveTest {
  private final AtomicBoolean passed = new AtomicBoolean(true);

  public DEV3688Test(TestConfig testConfig) {
    super(testConfig, DEV3688App.class, DEV3688App.class);
    testConfig.getL2Config().setPersistenceMode(PersistenceMode.PERMANENT_STORE);
  }

  @Override
  protected void startServers() throws Exception {
    // don't start passive server
    testServerManager.startServer(0, 0);
  }

  @Override
  protected void startClients() throws Throwable {
    /**
     * Steps to reproduce DEV-3688:<br>
     * <br>
     * 1. Start A which becomes ACTIVE<br>
     * 2. Start a client which creates root and does something<br>
     * 3. Stop A<br>
     * 4. Start A again which will again start as ACTIVE<br>
     * 5. Start B which will become PASSIVE<br>
     * 6. Stop A so B becomes ACTIVE<br>
     * 7. Start a new client now and you will see that the server has again given ObjectID=1000<br>
     */

    final int jmxPortA = testServerManager.getGroupData(0).getJmxPort(0);
    final int jmxPortB = testServerManager.getGroupData(0).getJmxPort(1);

    // already started by framework
    System.out.println("1. Start A which becomes ACTIVE");

    System.out.println("2. Start a client which creates root and does something");
    Thread client1 = startClient(DEV3688App.class);
    Thread.sleep(5000);

    System.out.println("3. Stop A");
    testServerManager.crashActiveServer(0);

    Thread.sleep(500);
    System.out.println("4. Start A again which will again start as ACTIVE");
    testServerManager.startServer(0, 0);
    waitTillBecomeActive(jmxPortA);

    System.out.println("5. Start B which will become PASSIVE");
    testServerManager.startServer(0, 1);
    waitTillBecomePassiveStandBy(jmxPortB);

    System.out.println("6. Stop A so B becomes ACTIVE");
    testServerManager.crashActiveServer(0);
    waitTillBecomeActive(jmxPortB);

    System.out.println("7. Start a new client now and the server should not give ObjectID=1000");

    Thread client2 = startClient(DEV3688App.class);
    client1.join();
    client2.join();

  }

  private Thread startClient(final Class<? extends ClientBase> client) {
    Thread th = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          runClient(client);
        } catch (Throwable e) {
          e.printStackTrace();
          passed.set(false);
        }
      }
    }, client.getSimpleName());
    th.start();
    return th;
  }

  private void waitTillBecomeActive(int jmxPort) {
    while (true) {
      if (isActive(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private void waitTillBecomePassiveStandBy(int jmxPort) {
    while (true) {
      if (isPassiveStandBy(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private boolean isActive(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isActive();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private boolean isPassiveStandBy(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isPassiveStandBy = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isPassiveStandBy = mbean.isPassiveStandby();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isPassiveStandBy;
  }

}
