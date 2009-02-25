/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.management.exposed.TerracottaClusterMBean;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractTransparentApp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;

public class ClusterMembershipEventJMXTestApp extends AbstractTransparentApp implements NotificationListener {

  public static final String      CONFIG_FILE    = "config-file";
  public static final String      PORT_NUMBER    = "port-number";
  public static final String      HOST_NAME      = "host-name";

//  private final ApplicationConfig config;

  private MBeanServer             server         = null;
  private ObjectName              clusterBean    = null;
  private final List                    clusterBeanBag = new ArrayList();
  private final Map                     eventsCount    = new HashMap();

  public ClusterMembershipEventJMXTestApp(final String appId, final ApplicationConfig config, final ListenerProvider listenerProvider) {
    super(appId, config, listenerProvider);
//    this.config = config;

    try {
      registerMBeanListener();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public static void visitL1DSOConfig(final ConfigVisitor visitor, final DSOClientConfigHelper config) {
    String testClass = ClusterMembershipEventJMXTestApp.class.getName();
    String methodExpression = "* " + testClass + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    config.addIncludePattern(testClass + "$*");
  }

  public void run() {
    try {
      runTest();
    } catch (Throwable t) {
      notifyError(t);
    }
  }

  private void runTest() throws Throwable {
    TerracottaClusterMBean cluster = (TerracottaClusterMBean)MBeanServerInvocationHandler
      .newProxyInstance(server, clusterBean, TerracottaClusterMBean.class, false);

    try {
      cluster.getNodeId();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (RuntimeMBeanException e) {
      Assert.assertTrue(e.getCause() instanceof UnsupportedOperationException);
    }

    try {
      cluster.getNodesInCluster();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (RuntimeMBeanException e) {
      Assert.assertTrue(e.getCause() instanceof UnsupportedOperationException);
    }

    try {
      cluster.isConnected();
      Assert.fail("Expected UnsupportedOperationException");
    } catch (RuntimeMBeanException e) {
      Assert.assertTrue(e.getCause() instanceof UnsupportedOperationException);
    }

    // Disabling entire test since JMX event notifications have been deprecated
    /*
    config.getServerControl().crash();
    while (config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    // this sleep should be longer than l1-reconnect timeout
    Thread.sleep(30 * 1000);
    config.getServerControl().start();
    while (!config.getServerControl().isRunning()) {
      Thread.sleep(5000);
    }
    echo("Server restarted successfully.");
    spawnNewClient();
    synchronized (eventsCount) {
      while (eventsCount.size() < 4) {
        eventsCount.wait(3 * 60 * 1000);
      }
      Assert.assertTrue(((Integer) eventsCount.get("com.tc.cluster.event.nodeDisconnected")).intValue() >= 1);
      Assert.assertTrue(((Integer) eventsCount.get("com.tc.cluster.event.nodeConnected")).intValue() >= 1);
      Assert.assertTrue(((Integer) eventsCount.get("com.tc.cluster.event.thisNodeDisconnected")).intValue() >= 1);
      Assert.assertTrue(((Integer) eventsCount.get("com.tc.cluster.event.thisNodeConnected")).intValue() >= 1);
    }
    */
  }

  private void registerMBeanListener() throws Exception {
    List servers = MBeanServerFactory.findMBeanServer(null);
    if (servers.size() == 0) { throw new RuntimeException("No bean server found!"); }
    echo("Servers found: " + servers.size());
    Assert.assertEquals(1, servers.size());
    server = (MBeanServer) servers.get(0);

    // our *star* bean for memebership events
    clusterBean = new ObjectName("org.terracotta:type=Terracotta Cluster,name=Terracotta Cluster Bean");

    // The MBeanServerDelegate emits notifications about
    // registration/unregistration of MBeans
    ObjectName delegateName = ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");

    // listener for newly registered MBeans
    NotificationListener listener = new NotificationListener() {
      public void handleNotification(final Notification notification, final Object handback) {
        synchronized (clusterBeanBag) {
          clusterBeanBag.add(handback);
          clusterBeanBag.notifyAll();
        }
      }
    };

    // filter to let only clusterBean passed through
    NotificationFilter filter = new NotificationFilter() {
      public boolean isNotificationEnabled(final Notification notification) {
        if (notification.getType().equals("JMX.mbean.registered")
            && ((MBeanServerNotification) notification).getMBeanName().equals(clusterBean)) return true;
        return false;
      }
    };

    // add our listener for clusterBean's registration
    server.addNotificationListener(delegateName, listener, filter, clusterBean);

    // because of race condition, clusterBean might already have registered
    // before we registered the listener
    Set allObjectNames = server.queryNames(null, null);

    if (!allObjectNames.contains(clusterBean)) {
      synchronized (clusterBeanBag) {
        while (clusterBeanBag.isEmpty()) {
          clusterBeanBag.wait();
        }
      }
    }

    // clusterBean is now registered, no need to listen for it
    server.removeNotificationListener(delegateName, listener);

    // now that we have the clusterBean, add listener for membership events
    try {
      server.addNotificationListener(clusterBean, this, null, clusterBean);
      Assert.fail("Expected UnsupportedOperationException");
    } catch (UnsupportedOperationException e) {
      // expected
    }
  }

  public void handleNotification(final Notification notification, final Object handback) {
    synchronized (eventsCount) {
      String msgType = notification.getType();
      if (eventsCount.containsKey(msgType)) {
        Integer count = (Integer) eventsCount.get(msgType);
        eventsCount.put(msgType, new Integer(count.intValue() + 1));
      } else {
        eventsCount.put(msgType, new Integer(1));
      }
      echo("type=" + notification.getType() + ", message=" + notification.getMessage());
      eventsCount.notifyAll();
    }
  }

  private static void echo(final String msg) {
    System.out.println(msg);
  }

  public static class L1Client {
    public static void main(final String args[]) {
      // nothing to do
    }
  }
  /*
  private ExtraL1ProcessControl spawnNewClient() throws Exception {
    final String hostName = config.getAttribute(HOST_NAME);
    final int port = Integer.parseInt(config.getAttribute(PORT_NUMBER));
    final File configFile = new File(config.getAttribute(CONFIG_FILE));
    File workingDir = new File(configFile.getParentFile(), "client-0");
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, L1Client.class, configFile
        .getAbsolutePath(), new String[0], workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    client.waitFor();
    System.err.println("\n### Started New Client");
    return client;
  }

  private void addTestTcPropertiesFile(final List jvmArgs) {
    URL url = getClass().getResource("/com/tc/properties/tests.properties");
    if (url == null) { return; }
    String pathToTestTcProperties = url.getPath();
    if (pathToTestTcProperties == null || pathToTestTcProperties.equals("")) { return; }
    System.err.println("\n##### -Dcom.tc.properties=" + pathToTestTcProperties);
    jvmArgs.add("-Dcom.tc.properties=" + pathToTestTcProperties);
  }
 */
}
