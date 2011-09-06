/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.beans.object.ObjectManagementMonitorMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.api.GCStats;
import com.tc.stats.api.DSOMBean;
import com.tc.test.JMXUtils;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class DGCSequencePersistenceActivePassiveTest extends BaseDSOTestCase {

  private final String    HOST = "localhost";

  private TcConfigBuilder configBuilder;
  private ExternalDsoServer server_1, server_2;
  private int               jmxPort_1, jmxPort_2;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/dgc-sequence-persistence-ap-test.xml");
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer("server-1");
    server_2 = createServer("server-2");
  }

  public void testDGCSequencePersistence() throws Exception {
    int iteration1 = 5, iteration2 = 8, iteration3 = 10, iteration4 = 7;

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");

    for (int i = 0; i < iteration1; i++)
      runGC(jmxPort_1);

    GCStats[] stats = collectGCStats(jmxPort_1);
    Assert.assertEquals(iteration1, stats.length);

    server_1.stop();

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");

    stats = collectGCStats(jmxPort_1);
    Assert.assertEquals(0, stats.length);

    for (int i = 0; i < iteration2; i++)
      runGC(jmxPort_1);
    stats = collectGCStats(jmxPort_1);

    Assert.assertEquals(iteration2, stats.length);

    for (int i = 0; i < stats.length; i++)
      Assert.assertEquals(iteration1 + i + 1, stats[i].getIteration());

    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");

    for (int i = 0; i < iteration3; i++)
      runGC(jmxPort_1);

    stats = collectGCStats(jmxPort_1);
    Assert.assertEquals(iteration2 + iteration3, stats.length);

    for (int i = 0; i < stats.length; i++)
      Assert.assertEquals(iteration1 + i + 1, stats[i].getIteration());

    stats = collectGCStats(jmxPort_2);
    Assert.assertEquals(0, stats.length);

    server_1.stop();

    waitTillBecomeActive(jmxPort_2);
    System.out.println("server 2 became active");

    stats = collectGCStats(jmxPort_2);
    Assert.assertEquals(0, stats.length);
    for (int i = 0; i < iteration4; i++)
      runGC(jmxPort_2);

    stats = collectGCStats(jmxPort_2);
    Assert.assertEquals(iteration4, stats.length);

    for (int i = 0; i < stats.length; i++)
      Assert.assertEquals(iteration1 + iteration2 + iteration3 + i + 1, stats[i].getIteration());
  }

  private GCStats[] collectGCStats(int jmxPort) throws Exception {
    JMXConnector jmxConnector = JMXUtils.getJMXConnector(HOST, jmxPort);
    MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, HOST, jmxPort);
    Assert.assertNotNull(mbs);
    DSOMBean mbean = getDSOMbean(mbs);

    GCStats[] gcStats = mbean.getGarbageCollectorStats();
    System.out.println("GC Stats Length : " + gcStats.length);

    closeJMXConnector(jmxConnector);
    return gcStats;
  }

  private void runGC(int jmxPort) throws Exception {
    boolean result = false;
    while (!result) {
      System.out.println("Going to run GC with jmxPort = " + jmxPort);
      JMXConnector jmxConnector = JMXUtils.getJMXConnector(HOST, jmxPort);
      MBeanServerConnection mbs = getMBeanServerConnection(jmxConnector, HOST, jmxPort);
      Assert.assertNotNull(mbs);
      ObjectManagementMonitorMBean objectMonitorMbean = getObjectMonitorMbean(mbs);
      result = objectMonitorMbean.runGC();

      // wait till GC started
      while (result && !objectMonitorMbean.isGCStarted()) {
        ThreadUtil.reallySleep(10);
      }

      while (result && objectMonitorMbean.isGCRunning()) {
        // Sleep before checking isGCRunning() to give GC time to get started.
        ThreadUtil.reallySleep(1000);
      }

      closeJMXConnector(jmxConnector);
      if (!result) {
        ThreadUtil.reallySleep(1000);
      }
    }
  }

  private DSOMBean getDSOMbean(MBeanServerConnection mbs) {
    return MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.DSO, DSOMBean.class, false);
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

  private ObjectManagementMonitorMBean getObjectMonitorMbean(MBeanServerConnection mbs) {
    return MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.OBJECT_MANAGEMENT,
                                                    ObjectManagementMonitorMBean.class, false);
  }

  private void closeJMXConnector(JMXConnector jmxConnector) {
    try {
      jmxConnector.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
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
    boolean isPassiveStandBy = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", jmxPort);
      final MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      mbean = MBeanServerInvocationProxy
          .newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO, TCServerInfoMBean.class, false);
      isPassiveStandBy = mbean.isPassiveStandby();
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

    return isPassiveStandBy;
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
    if (server_1 != null && server_1.isInitialized()) server_1.stop();
    if (server_2 != null && server_2.isInitialized()) server_2.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

}
