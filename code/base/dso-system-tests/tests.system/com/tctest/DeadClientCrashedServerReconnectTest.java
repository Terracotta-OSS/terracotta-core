/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.cluster.Cluster;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.PauseListener;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.objectserver.control.ServerControl;
import com.tc.stats.DSOMBean;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class DeadClientCrashedServerReconnectTest extends BaseDSOTestCase {

  private TestPauseListener pauseListener;

  public DeadClientCrashedServerReconnectTest() {
    // this.disableAllUntil("3001-01-01");
  }

  public void setUp() {
    pauseListener = new TestPauseListener();
  }

  public void testClientsStayDeadAcrossRestarts() throws Exception {
    final boolean isCrashy = true; // so persistence will be on
    PortChooser portChooser = new PortChooser();
    List jvmArgs = new ArrayList();
    int proxyPort = portChooser.chooseRandomPort();

    RestartTestHelper helper = new RestartTestHelper(isCrashy,
                                                     new RestartTestEnvironment(this.getTempDirectory(), portChooser,
                                                                                RestartTestEnvironment.DEV_MODE),
                                                     jvmArgs);
    int dsoPort = helper.getServerPort();
    int jmxPort = helper.getAdminPort();
    ServerControl server = helper.getServerControl();

    ProxyConnectManagerImpl mgr = ProxyConnectManagerImpl.getManager();
    mgr.setDsoPort(dsoPort);
    mgr.setProxyPort(proxyPort);
    mgr.setupProxy();

    server.start();

    mgr.proxyUp();

    // config for client
    configFactory().addServerToL1Config(null, proxyPort, jmxPort);
    L1TVSConfigurationSetupManager manager = super.createL1ConfigManager();

    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelper(manager);
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);
    DistributedObjectClient client = new DistributedObjectClient(configHelper,
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(), components, NullManager
                                                                     .getInstance(), new Cluster());
    client.setPauseListener(pauseListener);
    client.start();

    // wait until client handshake is complete...
    pauseListener.waitUntilUnpaused();

    checkServerHasClients(1, jmxPort);

    Thread.sleep(5 * 1000);

    // disconnect the client from the server... this should make the server kill the client from the server
    // perspective...
    mgr.proxyDown();

    // give time for server to process client disconnect
    Thread.sleep(5 * 1000);

    // Now crash the server
    server.crash();

    // start the server back up
    server.start();
    mgr.proxyUp();

    // give time for jmx server to start up
    Thread.sleep(15 * 1000);

    assertTrue(pauseListener.isPaused());

    checkServerHasClients(0, jmxPort);
  }

  private void checkServerHasClients(int clientCount, int jmxPort) throws Exception {
    String url = "service:jmx:rmi:///jndi/rmi://localhost:" + jmxPort + "/jmxrmi";
    JMXServiceURL jmxServerUrl = new JMXServiceURL(url);
    JMXConnector jmxConnector = JMXConnectorFactory.newJMXConnector(jmxServerUrl, null);
    jmxConnector.connect();
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    DSOMBean mbean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DSO, DSOMBean.class,
                                                                              true);
    int actualClientCount = mbean.getClients().length;
    if (actualClientCount != clientCount) { throw new AssertionError(
                                                                     "Incorrect number of clients connected to the server: expected=["
                                                                         + clientCount + "] but was actual=["
                                                                         + actualClientCount + "]."); }

    System.out.println("***** " + clientCount + " clients are connected to the server.");
  }

  private static final class TestPauseListener implements PauseListener {

    private boolean paused = true;

    public void waitUntilPaused() throws InterruptedException {
      waitUntilCondition(true);
    }

    public void waitUntilUnpaused() throws InterruptedException {
      waitUntilCondition(false);
    }

    public boolean isPaused() {
      synchronized (this) {
        return paused;
      }
    }

    private void waitUntilCondition(boolean b) throws InterruptedException {
      synchronized (this) {
        while (b != paused) {
          wait();
        }
      }
    }

    public void notifyPause() {
      synchronized (this) {
        paused = true;
        notifyAll();
      }
    }

    public void notifyUnpause() {
      synchronized (this) {
        paused = false;
        notifyAll();
      }
    }

  }

}
