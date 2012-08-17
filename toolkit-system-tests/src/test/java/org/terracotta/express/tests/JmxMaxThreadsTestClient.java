/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests;

import org.terracotta.express.tests.base.ClientBase;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.toolkit.Toolkit;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.stats.api.DSOClientMBean;
import com.tc.stats.api.DSOMBean;

import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

public class JmxMaxThreadsTestClient extends ClientBase {

  public JmxMaxThreadsTestClient(String[] args) {
    super(args);
  }

  @Override
  public void test(Toolkit toolkit) {
    // Create threads and get l1 infos
    int noOfThreads = 20;

    Thread[] threads = new Thread[noOfThreads];
    for (int i = 0; i < noOfThreads; i++) {
      threads[i] = new Thread(new Runnable() {
        public void run() {
          getConfig();
        }
      });
    }

    for (int i = 0; i < noOfThreads; i++) {
      threads[i].start();
    }

    for (int i = 0; i < noOfThreads; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        //
      }
    }
  }

  private void getConfig() {
    JMXConnector jmxConnector = null;
    int jmxPort = getGroupData(0).getJmxPort(0);
    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
    } catch (Exception e) {
      new AssertionError(e);
    }
    MBeanServerConnection mbsc = getMBeanServerConnection(jmxConnector, "localhost", jmxPort);
    DSOMBean dsoMBean = MBeanServerInvocationHandler.newProxyInstance(mbsc, L2MBeanNames.DSO, DSOMBean.class, false);

    ObjectName[] clientObjectNames = dsoMBean.getClients();
    DSOClientMBean[] clients = new DSOClientMBean[clientObjectNames.length];
    for (int i = 0; i < clients.length; i++) {
      clients[i] = MBeanServerInvocationHandler.newProxyInstance(mbsc, clientObjectNames[i], DSOClientMBean.class,
                                                                 false);
      L1InfoMBean l1InfoMbean = MBeanServerInvocationHandler.newProxyInstance(mbsc, clients[i].getL1InfoBeanName(),
                                                                              L1InfoMBean.class, false);
      l1InfoMbean.getConfig();
    }
    closeJMXConnector(jmxConnector);
  }

  private static MBeanServerConnection getMBeanServerConnection(final JMXConnector jmxConnector, String host, int port) {
    MBeanServerConnection mbs;
    try {
      mbs = jmxConnector.getMBeanServerConnection();
    } catch (IOException e1) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta Server instance running there?");
      return null;
    }
    return mbs;
  }

  private static void closeJMXConnector(final JMXConnector jmxConnector) {
    try {
      jmxConnector.close();
    } catch (IOException e) {
      System.err.println("Unable to close the JMX connector " + e.getMessage());
    }
  }
}
