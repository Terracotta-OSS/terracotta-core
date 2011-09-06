/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.exception.TCRuntimeException;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.object.BaseDSOTestCase;
import com.tc.test.JMXUtils;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;
import com.tctest.process.ExternalDsoServer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class BindAddressesTest extends BaseDSOTestCase {
  private static final int          NUM_OF_SERVERS = 4;
  private TcConfigBuilder           configBuilder;
  private final ExternalDsoServer[] servers        = new ExternalDsoServer[NUM_OF_SERVERS];
  private final int[]               jmxPorts       = new int[NUM_OF_SERVERS];
  private final int[]               dsoPorts       = new int[NUM_OF_SERVERS];
  private final int[]               l2GroupPorts   = new int[NUM_OF_SERVERS];

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  static String localAddr() {
    try {
      String rv = InetAddress.getLocalHost().getHostAddress();
      if (rv.startsWith("127.")) { throw new RuntimeException("Wrong local address " + rv); }
      return rv;
    } catch (UnknownHostException uhe) {
      throw new TCRuntimeException("Host resolve error:" + uhe);
    }
  }

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tc/bind-address-test.xml");
    configBuilder.randomizePorts();

    for (int i = 0; i < NUM_OF_SERVERS; i++) {
      dsoPorts[i] = configBuilder.getDsoPort(i);
      jmxPorts[i] = configBuilder.getJmxPort(i);
      l2GroupPorts[i] = configBuilder.getGroupPort(i);
    }

    servers[0] = createServer("server-1");

    servers[0].start();
    System.out.println("server-1 started");
    waitTillBecomeActive(jmxPorts[0]);
    System.out.println("server-1 became active");

    for (int i = 1; i < NUM_OF_SERVERS; i++) {
      servers[i] = createServer("server-" + (i + 1));
      servers[i].start();
      waitTillBecomePassiveStandBy(jmxPorts[i]);
      System.out.println("server-" + (i + 1) + " became passive");
    }
  }

  public void testBind() throws Exception {
    testSocketConnect("127.0.0.1", dsoPorts[0], true);
    testSocketConnect("0.0.0.0", jmxPorts[0], true);
    testSocketConnect("0.0.0.0", l2GroupPorts[0], true);

    testSocket("127.0.0.1", dsoPorts[1], true);
    testSocket("127.0.0.1", jmxPorts[1], false);
    testSocket("127.0.0.1", l2GroupPorts[1], false);

    testSocket("127.0.0.1", dsoPorts[2], true);
    testSocket("127.0.0.1", jmxPorts[2], false);
    testSocket("127.0.0.1", l2GroupPorts[2], false);

    testSocket("0.0.0.0", dsoPorts[3], true);
    testSocket("0.0.0.0", jmxPorts[3], false);
    testSocket("0.0.0.0", l2GroupPorts[3], false);
  }

  private void testSocketConnect(String host, int port, boolean testNegative) throws Exception {
    InetAddress addr = InetAddress.getByName(host);
    if (addr.isAnyLocalAddress()) {
      // should be able to connect on both localhost and local IP
      testSocketConnect("127.0.0.1", port, false);
      testSocketConnect(localAddr(), port, false);
    } else {
      // positive case
      testSocket(host, port, false);

      if (testNegative) {
        // negative case
        if (addr.isLoopbackAddress()) {
          testSocket(localAddr(), port, true);
        } else if (InetAddress.getByName(localAddr()).equals(addr)) {
          testSocket("127.0.0.1", port, true);
        } else {
          throw new AssertionError(addr);
        }
      }
    }
  }

  private static void testSocket(String host, int port, boolean expectFailure) throws Exception {
    System.err.print("testing connect on " + host + ":" + port + " ");
    Socket s = null;
    try {
      s = new Socket(host, port);
      if (expectFailure) {
        System.err.println("[FAIL]");
        throw new AssertionError("should not connect");
      }
    } catch (Exception ioe) {
      if (!expectFailure) {
        System.err.println("[FAIL]");
        throw ioe;
      }
    } finally {
      closeQuietly(s);
    }

    System.err.println("[OK]");
  }

  private boolean isActive(int bindPort) {
    TCServerInfoMBean mbean = null;
    boolean isActive = false;
    JMXConnector jmxConnector = null;

    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", bindPort);
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
          System.out.println("Exception while trying to close the JMX connector for port no: " + bindPort);
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

  private void waitTillBecomeActive(int adminPort) {
    while (true) {
      if (isActive(adminPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private void waitTillBecomePassiveStandBy(int jmxPort) {
    while (true) {
      if (isPassiveStandBy(jmxPort)) break;
      ThreadUtil.reallySleep(1000);
    }
  }

  private static void closeQuietly(Socket s) {
    if (s == null) return;
    try {
      s.close();
    } catch (IOException ioe) {
      // ignore
    }
  }

  private ExternalDsoServer createServer(final String serverName) throws IOException {
    return new ExternalDsoServer(getWorkDir(serverName), configBuilder.newInputStream(), serverName);
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }
}
