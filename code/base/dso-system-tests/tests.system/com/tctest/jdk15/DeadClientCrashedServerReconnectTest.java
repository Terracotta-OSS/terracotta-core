/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.object.BaseDSOTestCase;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.logging.NullRuntimeLogger;
import com.tc.objectserver.control.ServerControl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.test.proxyconnect.ProxyConnectManager;
import com.tc.test.proxyconnect.ProxyConnectManagerImpl;
import com.tc.test.restart.RestartTestEnvironment;
import com.tc.test.restart.RestartTestHelper;
import com.tc.util.PortChooser;

import java.util.ArrayList;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.remote.JMXConnector;

public class DeadClientCrashedServerReconnectTest extends BaseDSOTestCase {
  private final TCProperties tcProps;

  public DeadClientCrashedServerReconnectTest() {
    tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED, "false");
    System.out.println("L1 and L2 config match check disabled temporarily as we use proxy");
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

    ProxyConnectManager mgr = new ProxyConnectManagerImpl();
    mgr.setDsoPort(dsoPort);
    mgr.setProxyPort(proxyPort);
    mgr.setupProxy();

    server.start();

    mgr.proxyUp();

    // config for client
    configFactory().addServerToL1Config(null, proxyPort, jmxPort);
    L1ConfigurationSetupManager manager = super.createL1ConfigManager();

    DSOClientConfigHelper configHelper = new StandardDSOClientConfigHelperImpl(manager);
    PreparedComponentsFromL2Connection components = new PreparedComponentsFromL2Connection(manager);
    DistributedObjectClient client = new DistributedObjectClient(configHelper,
                                                                 new TCThreadGroup(new ThrowableHandler(TCLogging
                                                                     .getLogger(DistributedObjectClient.class))),
                                                                 new MockClassProvider(), components,
                                                                 NullManager.getInstance(),
                                                                 new StatisticsAgentSubSystemImpl(),
                                                                 new DsoClusterImpl(), new NullRuntimeLogger());
    client.setCreateDedicatedMBeanServer(true);
    client.start();

    // wait until client handshake is complete...
    waitUntilUnpaused(client);

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

    checkServerHasClients(0, jmxPort);
  }

  private void waitUntilUnpaused(final DistributedObjectClient client) {
    ClientHandshakeManager mgr = client.getClientHandshakeManager();
    mgr.waitForHandshake();
  }

  private void checkServerHasClients(final int clientCount, final int jmxPort) throws Exception {
    JMXConnector jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
    MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
    DSOMBean mbean = (DSOMBean) MBeanServerInvocationHandler.newProxyInstance(mbs, L2MBeanNames.DSO, DSOMBean.class,
                                                                              true);
    int actualClientCount = mbean.getClients().length;
    if (actualClientCount != clientCount) { throw new AssertionError(
                                                                     "Incorrect number of clients connected to the server: expected=["
                                                                         + clientCount + "] but was actual=["
                                                                         + actualClientCount + "]."); }

    System.out.println("***** " + clientCount + " clients are connected to the server.");
    jmxConnector.close();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    tcProps.setProperty(TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED, "true");
    System.out.println("Re-enabling L1 and L2 config match check");

  }
}
