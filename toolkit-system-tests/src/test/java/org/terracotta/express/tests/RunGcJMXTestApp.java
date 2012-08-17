/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.toolkit.Toolkit;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

import junit.framework.Assert;

public class RunGcJMXTestApp extends ClientBase {

  public static final String           CONFIG_FILE = "config-file";
  public static final String           PORT_NUMBER = "port-number";
  public static final String           HOST_NAME   = "host-name";
  public static final String           JMX_PORT    = "jmx-port";

  private MBeanServerConnection        mbsc        = null;
  private JMXConnector                 jmxc;
  private ObjectManagementMonitorMBean objectMBean;

  public RunGcJMXTestApp(String[] args) {
    super(args);
  }

  public static void main(String[] args) {
    new RunGcJMXTestApp(args).run();
  }

  @Override
  protected void test(Toolkit toolkit) throws Throwable {

    for (int i = 0; i < 10; i++) {
      try {
        runGC();
      } catch (Exception e) {
        System.out.println(e.getMessage());
      }

      Assert.assertTrue(getTestControlMbean().isActivePresent(0));
    }

  }

  private void connect() throws Exception {
    System.out.println("connecting to jmx server....");
    int jmxPort = getTestControlMbean().getGroupsData()[0].getJmxPort(0);
    jmxc = JMXUtils.getJMXConnector("localhost", jmxPort);
    mbsc = jmxc.getMBeanServerConnection();
    System.out.println("obtained mbeanserver connection");
    objectMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.OBJECT_MANAGEMENT,
                                                                ObjectManagementMonitorMBean.class, false);

  }

  private void disconnect() throws Exception {
    if (jmxc != null) {
      jmxc.close();
    }
  }

  private void runGC() throws Exception {
    connect();
    objectMBean.runGC();
    disconnect();
  }

}
