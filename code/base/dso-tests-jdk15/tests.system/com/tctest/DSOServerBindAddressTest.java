/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import com.tc.config.schema.SettableConfigItem;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.exception.TCRuntimeException;
import com.tc.lang.StartupHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.server.NullTCServerInfo;
import com.tc.util.Assert;
import com.tc.util.PortChooser;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Test for DEV-1060
 *
 * @author Manoj
 */
public class DSOServerBindAddressTest extends BaseDSOTestCase {
  private TCThreadGroup           group     = new TCThreadGroup(new ThrowableHandler(TCLogging
                                                .getLogger(DistributedObjectServer.class)));
  private static final String[]   bindAddrs = { "0.0.0.0", "127.0.0.1", localAddr() };
  private DistributedObjectServer server;
  
  static String localAddr() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException uhe) {
      throw new TCRuntimeException("Host resolve error:" + uhe);
    }
  }

  private class StartAction implements StartupAction {
    private final int    dsoPort;
    private final int    jmxPort;
    private final String bindAddr;

    public StartAction(String bindAddr, int dsoPort, int jmxPort) {
      this.bindAddr = bindAddr;
      this.dsoPort = dsoPort;
      this.jmxPort = jmxPort;
    }

    public void execute() throws Throwable {
      server = new DistributedObjectServer(createL2Manager(bindAddr, dsoPort, jmxPort), group,
                                           new NullConnectionPolicy(), new NullTCServerInfo());
      server.start();
    }

  }

  public void testDSOServerAndJMXBindAddress() throws Exception {
    PortChooser pc = new PortChooser();

    ManagedObjectStateFactory.disableSingleton(true);

    for (int i = 0; i < bindAddrs.length; i++) {
      String bind = bindAddrs[i];
      int dsoPort = pc.chooseRandomPort();
      int jmxPort = pc.chooseRandomPort();

      new StartupHelper(group, new StartAction(bind, dsoPort, jmxPort)).startUp();
      if (i == 0) {
        Assert.eval(server.getListenAddr().isAnyLocalAddress());
      } else {
        assertEquals(server.getListenAddr().getHostAddress(), bind);
      }
      Assert.assertNotNull(server.getJMXConnServer());
      assertEquals(server.getJMXConnServer().getAddress().getHost(), bind);

      testSocketConnect(bind, new int[] { dsoPort, jmxPort }, true);

      server.stop();
      Thread.sleep(3000);
    }
  }

  private void testSocketConnect(String host, int[] ports, boolean testNegative) throws IOException {
    InetAddress addr = InetAddress.getByName(host);
    if (addr.isAnyLocalAddress()) {
      // should be able to connect on both localhost and local IP
      testSocketConnect("127.0.0.1", ports, false);
      testSocketConnect(localAddr(), ports, false);
    } else {
      // positive case
      for (int i = 0; i < ports.length; i++) {
        testSocket(host, ports[i], false);
      }

      if (testNegative) {
        // negative case
        for (int i = 0; i < ports.length; i++) {
          if (addr.isLoopbackAddress()) {
            testSocket(localAddr(), ports[i], true);
          } else if (InetAddress.getByName(localAddr()).equals(addr)) {
            testSocket("127.0.0.1", ports[i], true);
          } else {
            throw new AssertionError(addr);
          }
        }
      }
    }
  }

  private static void testSocket(String host, int port, boolean expectFailure) throws IOException {
    System.err.print("testing connect on " + host + ":" + port + " ");
    Socket s = null;
    try {
      s = new Socket(host, port);
      if (expectFailure) {
        System.err.println("[FAIL]");
        throw new AssertionError("should not connect");
      }
    } catch (IOException ioe) {
      if (!expectFailure) {
        System.err.println("[FAIL]");
        throw ioe;
      }
    } finally {
      closeQuietly(s);
    }

    System.err.println("[OK]");
  }

  private static void closeQuietly(Socket s) {
    if (s == null) return;
    try {
      s.close();
    } catch (IOException ioe) {
      // ignore
    }
  }

  public L2TVSConfigurationSetupManager createL2Manager(String bindAddress, int dsoPort, int jmxPort)
      throws ConfigurationSetupException {
    TestTVSConfigurationSetupManagerFactory factory = super.configFactory();
    L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
    ((SettableConfigItem) factory.l2DSOConfig().bind()).setValue(bindAddress);
    ((SettableConfigItem) factory.l2DSOConfig().listenPort()).setValue(dsoPort);
    ((SettableConfigItem) factory.l2CommonConfig().jmxPort()).setValue(jmxPort);
    return manager;
  }
}
