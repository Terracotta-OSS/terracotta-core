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

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.net.TCSocketAddress;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.control.VerboseGCHelper;
import com.tc.properties.TCPropertiesConsts;
import com.tc.test.JMXUtils;
import com.tc.test.process.ExternalDsoClient;
import com.tc.test.process.ExternalDsoServer;
import com.tc.util.Assert;
import com.tc.util.TcConfigBuilder;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class BindAddressSystemTest extends BaseDSOTestCase {
  private File            tcConfig = null;
  private TcConfigBuilder configBuilder;
  private ExternalDsoServer server_1, server_2;
  private int               jmxPort_1, jmxPort_2;

  @Override
  protected boolean cleanTempDir() {
    return true;
  }

  public void testTsaPortBinding() throws Exception {
    tcConfig = getTempFile("server-tc-config-bind-address-test.xml");
    String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                    + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                    + "\n      <server jmx-enabled=\"true\" name=\"server1\" host=\""
                    + TCSocketAddress.LOOPBACK_IP
                    + "\">"
                    + "\n      <tsa-port bind=\""
                    + InetAddress.getLocalHost().getHostAddress()
 + "\">9510</tsa-port>"
                    + "\n      <dataStorage size=\"512m\"/>"
                    + "\n      </server>"
                    + "\n      <server jmx-enabled=\"true\" name=\"server2\" host=\""
                    + TCSocketAddress.LOOPBACK_IP
                    + "\">"
                    + "\n      <tsa-port bind=\""
                    + InetAddress.getLocalHost().getHostAddress()
                    + "\">8510</tsa-port>"
                    + "\n      <dataStorage size=\"512m\"/>"
                    + "\n      </server>"
                    + "\n</servers>" + "\n</tc:tc-config>";
    writeConfigFile(config);
    configBuilder = new TcConfigBuilder(tcConfig.getAbsoluteFile());
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer("server1");
    server_2 = createServer("server2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");

    ExternalDsoClient client = createClient("client");
    client.start();
    ThreadUtil.reallySleep(5000);
    while (client.isRunning()) {
      ThreadUtil.reallySleep(1000);
    }
  }

  public void testGroupPortBinding() throws Exception {
    tcConfig = getTempFile("server-tc-config-bind-address-test.xml");
    String config = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>"
                    + "\n<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "\n<servers>"
                    + "\n      <server jmx-enabled=\"true\" name=\"server1\" host=\""
                    + TCSocketAddress.LOOPBACK_IP
                    + "\">"
                    + "\n      <tsa-group-port bind=\""
                    + InetAddress.getLocalHost().getHostAddress()
                    + "\">9510</tsa-group-port>"
                    + "\n      <dataStorage size=\"512m\"/>"
                    + "\n      </server>"
                    + "\n      <server jmx-enabled=\"true\" name=\"server2\" host=\""
                    + TCSocketAddress.LOOPBACK_IP
                    + "\">"
                    + "\n      <tsa-group-port bind=\""
                    + InetAddress.getLocalHost().getHostAddress()
                    + "\">8510</tsa-group-port>"
                    + "\n      <dataStorage size=\"512m\"/>"
                    + "\n      </server>"
                    + "\n</servers>" + "\n</tc:tc-config>";
    writeConfigFile(config);
    configBuilder = new TcConfigBuilder(tcConfig.getAbsoluteFile());
    configBuilder.randomizePorts();

    jmxPort_1 = configBuilder.getJmxPort(0);
    jmxPort_2 = configBuilder.getJmxPort(1);

    server_1 = createServer("server1");
    server_2 = createServer("server2");

    server_1.start();
    System.out.println("server1 started");
    waitTillBecomeActive(jmxPort_1);
    System.out.println("server1 became active");
    server_2.start();
    System.out.println("server2 started");
    waitTillBecomePassiveStandBy(jmxPort_2);
    System.out.println("server2 became passive");
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
    VerboseGCHelper.getInstance().setupTempDir(getTempDirectory());
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

  private ExternalDsoClient createClient(final String name) throws IOException {
    ExternalDsoClient client = new ExternalDsoClient(name, getWorkDir(name), configBuilder.newInputStream(), L1.class);
    client.addJvmArg("-Dl1.name=" + name);
    client.addJvmArg("-Dcom.tc." + TCPropertiesConsts.L1_L2_CONFIG_VALIDATION_ENABLED + "=false");
    return client;
  }

  public static class L1 {
    public static void main(final String[] args) {
      System.out.println(System.getProperty("l1.name") + ": started");
      System.out.println(System.getProperty("l1.name") + ": stopped");
    }
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
}
