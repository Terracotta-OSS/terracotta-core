/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class ClusterMemberhipEventsTest extends BaseDSOTestCase {
  private TcConfigBuilder     configBuilder;
  private ExternalDsoServer   server_1, server_2;
  private int                 jmxPort_1, jmxPort_2;
  private static final String CONFIG = "/com/tc/cluster-event-test.xml";

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder(CONFIG);
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer("server-1");
    server_2 = createServer("server-2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");

  }

  public void testClusterEvents() throws Exception {
    int numOfClients = 5;
    ExtraL1ProcessControl[] clients = new ExtraL1ProcessControl[5];

    for (int i = 0; i < numOfClients - 1; i++) {
      clients[i] = createClient(i, numOfClients, configBuilder.getDsoPort(1), configBuilder.getJmxPort(1));
      clients[i].start();
      clients[i].mergeSTDOUT();
      clients[i].mergeSTDERR();
    }
    waitForClientsToStart(numOfClients - 1);
    System.out.println("all clients got connected...");
    ThreadUtil.reallySleep(5000);
    server_1.stop();
    clients[numOfClients - 1] = createClient(4, 5, configBuilder.getDsoPort(1), configBuilder.getJmxPort(1));
    clients[numOfClients - 1].start();
    clients[numOfClients - 1].mergeSTDOUT();
    clients[numOfClients - 1].mergeSTDERR();

    waitForClientsToFinish(clients);
  }

  private void waitForClientsToStart(int numberOfClients) {
    DSOMBean mbean = null;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", this.jmxPort_1);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.DSO, DSOMBean.class, false);
      while (true) {
        if (mbean.getClients().length == numberOfClients) break;
        ThreadUtil.reallySleep(2000);
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + this.jmxPort_1);
        }
      }
    }

  }

  private void waitForClientsToFinish(ExtraL1ProcessControl[] clients) throws Exception {
    for (ExtraL1ProcessControl client : clients) {
      client.waitUntilShutdown();
    }
  }

  private boolean isActive(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isActive();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private boolean isPassiveStandBy(int jmxPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isActive = mbean.isPassiveStandby();
    } catch (Exception e) {
      return false;
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + jmxPort);
        }
      }
    }

    return isActive;
  }

  private void waitTillBecomeActive(int jmxPort) {
    while (true) {
      if (isActive(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private void waitTillBecomePassiveStandBy(int jmxPort) {
    while (true) {
      if (isPassiveStandBy(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private ExternalDsoServer createServer(final String serverName) throws IOException {
    ExternalDsoServer server = new ExternalDsoServer(getWorkDir(serverName), configBuilder.newInputStream(), serverName);
    return server;
  }

  @Override
  protected void tearDown() throws Exception {
    System.err.println("in tearDown");
    if (server_1 != null) server_1.stop();
    if (server_2 != null) server_2.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

  private ExtraL1ProcessControl createClient(int clientIndex, int numOfClients, int dsoPort, int adminPort)
      throws IOException {
    List jvmArgs = new ArrayList();
    File configFile = saveToFile(configBuilder.newInputStream());
    jvmArgs.add("-Dtc.node-name=node" + clientIndex);
    jvmArgs.add("-Dtc.config=" + configFile.getAbsolutePath());
    ExtraL1ProcessControl client = new ExtraL1ProcessControl("localhost", dsoPort, MailBox.class,
                                                             configFile.getAbsolutePath(),
                                                             Arrays.asList("" + clientIndex, "" + numOfClients),
                                                             getWorkDir("l1client" + clientIndex), jvmArgs);
    return client;
  }

  private File saveToFile(InputStream configInput) throws IOException {
    File config = new File(getTempDirectory(), "cluster-event-test.xml");
    FileOutputStream out = new FileOutputStream(config);
    IOUtils.copy(configInput, out);
    out.close();

    System.out.println("written to: " + config.getAbsolutePath());
    return config;
  }

  public static class MailBox {
    private final CyclicBarrier barrier;
    private final int           participantCount;
    private final int           clientIndex;

    public MailBox(int clientIndex, int participantCount) {
      this.clientIndex = clientIndex;
      this.participantCount = participantCount;
      this.barrier = new CyclicBarrier(this.participantCount);
    }

    private void start() {
      System.out.println("Client " + this.clientIndex + " waiting for all the clients to start");
      try {
        barrier.await();
      } catch (Exception e) {
        throw new AssertionError(e);
      }
      System.out.println("Client " + this.clientIndex + " finished");
    }

    public static void main(String[] args) {
      MailBox mailBox = new MailBox(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
      mailBox.start();
    }

  }

}
