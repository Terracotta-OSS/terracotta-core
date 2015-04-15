/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tctest;

import com.tc.exception.TCRuntimeException;
import com.tc.object.BaseDSOTestCase;
import com.tc.server.util.ServerStat;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class BindAddressesTest extends BaseDSOTestCase {
  private static final int          NUM_OF_SERVERS = 4;
  private TcConfigBuilder           configBuilder;
  private final ExternalDsoServer[] servers        = new ExternalDsoServer[NUM_OF_SERVERS];
  private final int[]               jmxPorts       = new int[NUM_OF_SERVERS];
  private final int[]               tsaPorts       = new int[NUM_OF_SERVERS];
  private final int[]               tsaGroupPorts  = new int[NUM_OF_SERVERS];
  private final int[]               managementPorts = new int[NUM_OF_SERVERS];

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
      tsaPorts[i] = configBuilder.getTsaPort(i);
      jmxPorts[i] = configBuilder.getJmxPort(i);
      tsaGroupPorts[i] = configBuilder.getGroupPort(i);
      managementPorts[i] = configBuilder.getManagementPort(i);
    }

    servers[0] = createServer("server-1");

    servers[0].start();
    System.out.println("server-1 started");
    waitTillBecomeActive(managementPorts[0]);
    System.out.println("server-1 became active");

    for (int i = 1; i < NUM_OF_SERVERS; i++) {
      servers[i] = createServer("server-" + (i + 1));
      servers[i].start();
      waitTillBecomePassiveStandBy(managementPorts[i]);
      System.out.println("server-" + (i + 1) + " became passive");
    }
  }

  public void testBind() throws Exception {
    testSocketConnect("127.0.0.1", tsaPorts[0], true);
    testSocketConnect("0.0.0.0", jmxPorts[0], true);
    testSocketConnect("0.0.0.0", tsaGroupPorts[0], true);
    testSocketConnect("0.0.0.0", managementPorts[0], true);

    testSocket("127.0.0.1", tsaPorts[1], true);
    testSocket("127.0.0.1", jmxPorts[1], false);
    testSocket("127.0.0.1", tsaGroupPorts[1], false);
    testSocket("127.0.0.1", managementPorts[1], false);

    testSocket("127.0.0.1", tsaPorts[2], true);
    testSocket("127.0.0.1", jmxPorts[2], false);
    testSocket("127.0.0.1", tsaGroupPorts[2], false);
    testSocket("127.0.0.1", managementPorts[2], false);

    testSocket("0.0.0.0", tsaPorts[3], true);
    testSocket("0.0.0.0", jmxPorts[3], false);
    testSocket("0.0.0.0", tsaGroupPorts[3], false);
    testSocket("0.0.0.0", managementPorts[3], false);
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
    try {
      ServerStat serverStat = ServerStat.getStats("localhost", bindPort, null, null, false, false);
      return "ACTIVE-COORDINATOR".equals(serverStat.getState());
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isPassiveStandBy(int managementPort) {
    try {
      ServerStat serverStat = ServerStat.getStats("localhost", managementPort, null, null, false, false);
      return "PASSIVE-STANDBY".equals(serverStat.getState());
    } catch (Exception e) {
      return false;
    }
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
