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

import com.tc.lcp.LinkedJavaProcess;
import com.tc.net.Netstat;
import com.tc.net.Netstat.SocketConnection;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.control.ServerMBeanRetriever;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.CallableWaiter;
import com.tc.util.TcConfigBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class HTTPConnectionLeakTest extends BaseDSOTestCase {
  private TcConfigBuilder      configBuilder;
  private ExternalDsoServer    server;
  private int                  jmxPort;
  private int                  tsaPort;
  private ServerMBeanRetriever serverMBeanRetriever;

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tctest/tc-one-server-config.xml");
    configBuilder.randomizePorts();
    server = new ExternalDsoServer(getWorkDir("server1"), configBuilder.newInputStream(), "server1");
    jmxPort = configBuilder.getJmxPort(0);
    tsaPort = configBuilder.getTsaPort(0);
    serverMBeanRetriever = new ServerMBeanRetriever("localhost", jmxPort);
    server.start();
    System.out.println("server1 started");
    waitTillBecomeActive();
    System.out.println("server1 became active");
  }

  public void testLeak() throws Exception {
    int initialConnectionCount = getNetInfoEstablishedConnectionsCount(tsaPort);
    System.out.println("http://localhost:" + tsaPort + "/config");
    for (int i = 0; i < 20; i++) {
      fetchConfig();
    }
    int finalConnectionCount = getNetInfoEstablishedConnectionsCount(tsaPort);
    System.out.println("initialConnectioncount : " + initialConnectionCount + " finalConnectionCount : "
                       + finalConnectionCount);
    Assert.assertEquals(initialConnectionCount, finalConnectionCount);
  }

  private void fetchConfig() throws Exception {
    List<String> URLsToFetch = new ArrayList<String>();
    URLsToFetch.add("http://localhost:" + tsaPort + "/config");
    LinkedJavaProcess fetchURLProcess = new LinkedJavaProcess(URLFetcher.class.getName(), URLsToFetch, null);
    fetchURLProcess.start();
    Result result = Exec.execute(fetchURLProcess, fetchURLProcess.getCommand(), null, null, null);
    if (result.getExitCode() != 0) { throw new AssertionError("URLFetcher Exit code is " + result.getExitCode()); }
  }

  private int getNetInfoEstablishedConnectionsCount(int bindPort) {
    int establishedConnections = 0;

    List<SocketConnection> connections = new ArrayList<Netstat.SocketConnection>();
    connections.addAll(Netstat.getEstablishedTcpConnections());
    connections.addAll(Netstat.getCloseWaitTcpConnections());

    System.out.println("XXX Established connections if any");
    for (SocketConnection connection : connections) {
      long port = connection.getLocalPort();
      long remotePort = connection.getRemotePort();
      if ((bindPort == port || bindPort == remotePort) && connection.getLocalAddr().isLoopbackAddress()
          && connection.getRemoteAddr().isLoopbackAddress()) {
        establishedConnections++;
        System.out.println("XXX " + connection);
      }
    }
    return establishedConnections;
  }

  private void waitTillBecomeActive() throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return serverMBeanRetriever.getTCServerInfoMBean().isActive();
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    System.err.println("in tearDown");
    if (server != null) server.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

  public static class URLFetcher {
    public static void main(String[] args) throws Exception {
      for (String arg : args) {
        URL configURL = new URL(arg);
        BufferedReader in = new BufferedReader(new InputStreamReader(configURL.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
          System.out.println(inputLine);
        in.close();
      }
    }
  }
}
