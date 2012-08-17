/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.WaitUtil;
import org.terracotta.toolkit.Toolkit;

import com.tc.management.beans.L2MBeanNames;
import com.tc.stats.api.DSOMBean;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class ClientAbscondAfterServerCrashTestClient extends ClientBase {

  public ClientAbscondAfterServerCrashTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) throws Exception {

    int jmxPort = getGroupData(0).getJmxPort(0);
    // Wait till all clients join the game
    int id = getBarrierForAllClients().await();
    try {
      // checkServerHasClients waits till the clients join
      checkServerHasClients(2, jmxPort);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
    if (id == 0) {
      System.out.println("XXX Crashing the Server");
      getBarrierForAllClients().await(); // 1 first client will abscond now second client will take control from here.
      getTestControlMbean().crashActiveServer(0);
      WaitUtil.waitUntilCallableReturnsFalse(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          return getTestControlMbean().isActivePresent(0);
        }
      });

    } else {
      getBarrierForAllClients().await(); // 1 wait here until the other client kills the server and absconds.
      WaitUtil.waitUntilCallableReturnsFalse(new Callable<Boolean>() {
        @Override
        public Boolean call() throws Exception {
          return getTestControlMbean().isActivePresent(0);
        }
      });
      ThreadUtil.reallySleep(15 * 1000); // Sleeping for 15 seconds to ensure that client exits properly and server is
      // crashed.

      getTestControlMbean().reastartLastCrashedServer(0);

      WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {

        @Override
        public Boolean call() throws Exception {
          return getTestControlMbean().isActivePresent(0);
        }
      });
      // Let Server complete the reconnect window; This test uses 15 secs reconnect window
      ThreadUtil.reallySleep(15 * 1000);
      // buffer time
      ThreadUtil.reallySleep(5 * 1000);
      waitUntilDSOMbeanAvailable(2 * 60 * 1000, jmxPort);
      checkServerHasClients(1, jmxPort);
    }

  }

  private void waitUntilDSOMbeanAvailable(final long timeout, final int jmxPort) throws Exception {
    long start = System.nanoTime();
    while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < timeout) {
      DSOMBean dsoMBean = getDSOMbean(jmxPort);
      try {
        dsoMBean.getClients();
        System.out.println("Waiting for DSO MBean.....");
        return;
      } catch (Exception e) {
        // ignore
      }
      ThreadUtil.reallySleep(10 * 1000);
    }
    throw new AssertionError("Timed out waiting for DSOMBean to become available.");
  }

  private DSOMBean getDSOMbean(final int jmxPort) throws MalformedURLException, IOException {
    JMXConnector jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    return MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DSO, DSOMBean.class, true);
  }

  public static class AbscondingClient {
    public static void main(final String args[]) {
      System.out.println("XXX CLIENT" + args[0] + "STARTED");
      try {
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException e) {
        //
      }
    }
  }

  private void checkServerHasClients(final int clientCount, final int jmxPort) throws Exception {
    JMXConnector jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    DSOMBean mbean = MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DSO, DSOMBean.class, true);
    int actualClientCount = mbean.getClients().length;
    while (actualClientCount != clientCount) {
      System.out.println("XXX Expecting " + clientCount + " clients. Present connected clients " + actualClientCount
                         + ". sleeping ...");
      ThreadUtil.reallySleep(5000);
      actualClientCount = mbean.getClients().length;
    }
    System.out.println("XXX " + clientCount + " clients are connected to the server.");
    jmxConnector.close();
  }

}
