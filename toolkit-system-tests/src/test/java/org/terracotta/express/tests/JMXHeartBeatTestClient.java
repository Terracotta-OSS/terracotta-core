/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.toolkit.Toolkit;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class JMXHeartBeatTestClient extends ClientBase {

  public JMXHeartBeatTestClient(String[] args) {
    super(args);
  }

  private boolean isServerAlive() {
    boolean isAlive = false;
    JMXConnector jmxc = null;
    try {
      echo("connecting to jmx server....");
      int jmxPort = getGroupData(0).getJmxPort(0);
      jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
      MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
      echo("obtained mbeanserver connection");
      TCServerInfoMBean serverMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO,
                                                                                    TCServerInfoMBean.class, false);
      String result = serverMBean.getHealthStatus();
      echo("got health status: " + result);
      jmxc.close();
      isAlive = result.startsWith("OK");
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      if (jmxc != null) {
        try {
          jmxc.close();
        } catch (IOException e) {/**/
        }
      }
    }

    return isAlive;
  }

  private static void echo(String msg) {
    System.out.println(msg);
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {

    Assert.assertEquals(true, isServerAlive());
    echo("Server is alive");
    echo("About to crash server...");
    getTestControlMbean().crashActiveServer(0);
    // has to sleep longer than l1-reconnect timeout
    ThreadUtil.reallySleep(30 * 1000);
    Assert.assertEquals(false, isServerAlive());
    echo("Server is crashed.");
    echo("About to restart server");
    getTestControlMbean().reastartLastCrashedServer(0);
    while (!getTestControlMbean().isActivePresent(0)) {
      ThreadUtil.reallySleep(5 * 1000);
    }
    echo("Server restarted successfully.");
    Assert.assertEquals(true, isServerAlive());

  }

}
