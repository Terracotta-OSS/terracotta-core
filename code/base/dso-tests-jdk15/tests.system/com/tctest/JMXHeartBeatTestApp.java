/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class JMXHeartBeatTestApp extends AbstractTransparentApp {

  public static final String      CONFIG_FILE      = "config-file";
  public static final String      PORT_NUMBER      = "port-number";
  public static final String      HOST_NAME        = "host-name";
  public static final String      JMX_PORT         = "jmx-port";

  private final ApplicationConfig config;

  private final int               initialNodeCount = getParticipantCount();
  private final CyclicBarrier     stage1           = new CyclicBarrier(initialNodeCount);

  private MBeanServerConnection   mbsc             = null;
  private JMXConnector jmxc;
  private ObjectName              tcServerInfoBean = null;
  private List                    clusterBeanBag   = new ArrayList();
  private Map                     eventsCount      = new HashMap();

  public JMXHeartBeatTestApp(String appId, ApplicationConfig config, ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
    this.config = config;
  }

  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {

    String testClass = JMXHeartBeatTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    TransparencyClassSpec spec = config.getOrCreateSpec(testClass);
    config.addIncludePattern(testClass + "$*");

    // roots
    spec.addRoot("stage1", "stage1");
  }

  public void run() {
    try {
      createJMXConnection();
      TCServerInfoMBean bean = (TCServerInfoMBean) MBeanServerInvocationHandler
          .newProxyInstance(mbsc, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);      
      String result = bean.getHealthStatus();
      Assert.assertEquals("OK", result);
    } catch (Throwable t) {
      notifyError(t);
    }
    finally {
      if (jmxc != null)  {
        try {
          jmxc.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private void runTest() throws Throwable {
    spawnNewClient();

    config.getServerControl().crash();
    while (config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    config.getServerControl().start(30 * 1000);
    while (!config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    echo("Server restarted successfully.");
    stage1.await();
    synchronized (eventsCount) {
      Assert.assertEquals(4, eventsCount.size());
      Assert.assertTrue(eventsCount.containsKey("com.tc.cluster.event.nodeDisconnected"));
      Assert.assertTrue(eventsCount.containsKey("com.tc.cluster.event.nodeConnected"));
      Assert.assertTrue(eventsCount.containsKey("com.tc.cluster.event.thisNodeDisconnected"));
      Assert.assertTrue(eventsCount.containsKey("com.tc.cluster.event.thisNodeConnected"));
    }
  }

  private void createJMXConnection() throws Exception {
    String theUrl = "service:jmx:rmi:///jndi/rmi://localhost:" + config.getAttribute(JMX_PORT) + "/jmxrmi";
    System.err.println("$$$$$$ JMX URL: " + theUrl);
    JMXServiceURL url = new JMXServiceURL(theUrl);
    jmxc = JMXConnectorFactory.connect(url, null);    
    mbsc = jmxc.getMBeanServerConnection();
  }

  private static void echo(String msg) {
    System.out.println(msg);
  }

  public static class L1Client {
    public static void main(String args[]) {
      // nothing to do
    }
  }

  private ExtraL1ProcessControl spawnNewClient() throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-0");
    FileUtils.forceMkdir(workingDir);

    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class, configFile
        .getAbsolutePath(), new String[0], workingDir);
    client.start(20000);
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

}
