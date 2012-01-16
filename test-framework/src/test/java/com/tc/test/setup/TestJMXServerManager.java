package com.tc.test.setup;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import com.tc.test.jmx.TestHandler;
import com.tc.test.jmx.TestHandlerMBean;

public class TestJMXServerManager {

  private int              jmxServerPort;
  private TestHandlerMBean testHandlerMBean;

  public TestJMXServerManager(int jmxServerPort, TestHandlerMBean testHandlerMBean) {
    this.jmxServerPort = jmxServerPort;
    this.testHandlerMBean = testHandlerMBean;
  }

  public void startJMXServer() throws Exception {
    // Get the platform MBeanServer
    System.out.println("********** Starting test JMX server at port[" + jmxServerPort + "]");
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    try {
      // Uniquely identify the MBeans and register them with the platform MBeanServer
      ObjectName tectControlBeanName = TestHandler.TEST_SERVER_CONTROL_MBEAN;
      mbs.registerMBean(testHandlerMBean, tectControlBeanName);
      JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://" + "localhost" + ":" + this.jmxServerPort);
      JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
      cs.start();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public void stopJmxServer() throws Exception {
    System.out.println("********** stopping test JMX server at port[" + jmxServerPort + "]");
    MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
    try {
      // Uniquely identify the MBeans and register them with the platform MBeanServer
      ObjectName tectControlBeanName = TestHandler.TEST_SERVER_CONTROL_MBEAN;
      mbs.unregisterMBean(tectControlBeanName);
      JMXServiceURL url = new JMXServiceURL("service:jmx:jmxmp://" + "localhost" + ":" + this.jmxServerPort);
      JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
      cs.stop();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
  
  public int getJmxServerPort() {
    return jmxServerPort;
  }
}
