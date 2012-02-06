/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.tests.base;

import org.terracotta.test.util.JMXUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.test.jmx.TestHandler;
import com.tc.test.jmx.TestHandlerMBean;
import com.tc.test.setup.GroupsData;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public abstract class AbstractClientBase implements Runnable {

  public final int               HEAVY_CLIENT_TEST_TIME = 5 * 60 * 1000;

  private final TestHandlerMBean testControlMBean;

  abstract protected void doTest() throws Throwable;

  public AbstractClientBase(String args[]) {
    int index = 0;
    final int testControlMbeanPort = Integer.parseInt(args[index++]);

    try {
      JMXConnector jmxConnector = JMXUtils.getJMXConnector("localhost", testControlMbeanPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      testControlMBean = MBeanServerInvocationProxy.newMBeanProxy(mbs, TestHandler.TEST_SERVER_CONTROL_MBEAN,
                                                                  TestHandlerMBean.class, false);
    } catch (Exception e) {
      System.out.println("****** Exception while connecting to TestMBean Port[" + testControlMbeanPort + "]");
      throw new RuntimeException(e);
    }
  }

  public final void run() {
    try {
      doTest();
      pass();
      System.exit(0);
    } catch (Throwable t) {
      t.printStackTrace();
      try {
        getTestControlMbean().dumpClusterState();
      } catch (Exception e) {
        new Exception("Unabled to dump cluster state.", e).printStackTrace();
      }
      System.exit(1);
    }
  }

  protected void pass() {
    System.err.println("[PASS: " + getClass().getName() + "]");
  }

  public TestHandlerMBean getTestControlMbean() {
    return this.testControlMBean;
  }

  public GroupsData getGroupData(int groupIndex) {
    return this.testControlMBean.getGroupsData()[groupIndex];
  }

  public int getParticipantCount() {
    return this.testControlMBean.getParticipantCount();
  }

  public String getTerracottaUrl() {
    return this.testControlMBean.getTerracottaUrl();
  }
}
