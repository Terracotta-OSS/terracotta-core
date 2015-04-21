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

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.net.TCSocketAddress;
import com.tc.object.BaseDSOTestCase;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.server.util.ServerStat;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class TCStopBindAddressTest extends BaseDSOTestCase {
  private static final String SERVER_NAME_1      = "server1";
  private File                tcConfig           = null;
  private TcConfigBuilder     configBuilder;
  private ExternalDsoServer   server1;
  private int                 managementPort1;
  private final long          SHUTDOWN_WAIT_TIME = TimeUnit.NANOSECONDS.convert(120, TimeUnit.SECONDS);

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  @Override
  protected void setUp() throws Exception {
    PortChooser portChooser = new PortChooser();
    managementPort1 = portChooser.chooseRandomPort();
    int tsaPort = portChooser.chooseRandomPort();
    tcConfig = getTempFile("tc-stop-bind-address-test.xml");
    String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                    + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                    + "\n <server name=\"server1\" host=\""
                    + InetAddress.getLocalHost().getHostAddress()
                    + " \" bind=\"" + TCSocketAddress.LOOPBACK_IP + "\">" + "\n     " + " <tsa-port bind=\""
                    + InetAddress.getLocalHost().getHostAddress() + "\">" + tsaPort + "</tsa-port>" + "\n     "
                    + " <management-port bind=\"" + TCSocketAddress.LOOPBACK_IP + "\">" + managementPort1
                    + "</management-port>" + "\n<dataStorage size=\"512m\"/>" + "\n</server>"
                    + "\n</servers>" + "\n</tc:tc-config>";
    writeConfigFile(config);
    configBuilder = new TcConfigBuilder(tcConfig.getAbsoluteFile());


    server1 = createServer("server1");

    server1.startWithoutWait();
    System.out.println("XXX server1 started");
    waitTillBecomeActive(managementPort1);
    System.out.println("XXX server1 became active");

  }

  public void testServerForceStop() throws Throwable {
    stop(server1, SERVER_NAME_1);
    Assert.assertFalse(server1.isRunning());

  }

  private void stop(ExternalDsoServer server, String serverName) throws Throwable {
    System.out.println("XXX Going to stop server :" + server.getAdminPort());
    stop(server.getAdminPort(), getCommandLineArgsForStop(serverName, server.getConfigFile().getPath()));
    System.out.println("XXX stoped server");
  }

  private String[] getCommandLineArgsForStop(String serverName, String configFilePath) {
    return new String[] { StandardConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, configFilePath,
        StandardConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName};

  }

  private void stop(int jmxPort, String[] args) throws Throwable {
    System.out.println("XXX Calling  TCStop ");
    LinkedJavaProcess stopper = new LinkedJavaProcess("com.tc.admin.TCStop", Arrays.asList(args));
    try {
      stopper.start();
      Result result = Exec.execute(stopper, stopper.getCommand(), null, null, null);
      if (result.getExitCode() != 0) { throw new AssertionError("Error in TcStop Exit code is " + result.getExitCode()); }
    } catch (Exception e) {
      Assert.fail("Unable to Stop server");
    }
    System.out.println("XXX Called  TCStop ");
    try {
      waitUntilShutdown(jmxPort);
    } catch (Exception e) {
      System.out.println("Exception while stopping server :" + jmxPort);
      System.out.println(e);
    }
  }

  private void waitUntilShutdown(int managementPort) throws Exception {
    long start = System.nanoTime();
    long timeout = start + SHUTDOWN_WAIT_TIME;
    while (isRunning(managementPort)) {
      Thread.sleep(1000);
      if (System.nanoTime() > timeout) {
        System.out.println("Server was shutdown but still up after " + SHUTDOWN_WAIT_TIME);
        break;
      }
    }
  }

  private boolean isRunning(int jmxPort) {
    Socket socket = null;
    try {
      socket = new Socket(TCSocketAddress.LOOPBACK_IP, jmxPort);
      if (!socket.isConnected()) throw new AssertionError();
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }
  }

  private boolean isActive(int managementPort) {
    try {
      ServerStat serverStat = ServerStat.getStats("localhost", managementPort, null, null, false, false);
      return "ACTIVE-COORDINATOR".equals(serverStat.getState());
    } catch (Exception e) {
      return false;
    }
  }

  private void waitTillBecomeActive(int jmxPort) {
    while (true) {
      if (isActive(jmxPort)) break;
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
    if (server1 != null && server1.isRunning()) server1.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

}
