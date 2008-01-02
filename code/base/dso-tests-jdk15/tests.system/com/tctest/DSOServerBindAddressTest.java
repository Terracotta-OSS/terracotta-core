/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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

import java.net.InetAddress;
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
    private final int bindAddrs_index;

    public StartAction(int index) {
      bindAddrs_index = index;
    }

    public void execute() throws Throwable {
      server = new DistributedObjectServer(createL2Manager(bindAddrs[bindAddrs_index]), group,
                                           new NullConnectionPolicy(), new NullTCServerInfo());
      server.start();
    }

  }

  public void testDSOServerAndJMXBindAddress() throws Exception {
    ManagedObjectStateFactory.disableSingleton(true);

    for (int i = 0; i < bindAddrs.length; i++) {
      new StartupHelper(group, new StartAction(i)).startUp();
      if (i == 0) {
        Assert.eval(server.getListenAddr().isAnyLocalAddress());
      } else {
        assertEquals(server.getListenAddr().getHostAddress(), bindAddrs[i]);
      }
      Assert.assertNotNull(server.getJMXConnServer());
      assertEquals(server.getJMXConnServer().getAddress().getHost(), bindAddrs[i]);
      server.stop();
      Thread.sleep(3000);
    }
  }

  public L2TVSConfigurationSetupManager createL2Manager(String l2Host) throws ConfigurationSetupException {
    TestTVSConfigurationSetupManagerFactory factory = super.configFactory();
    L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(null);
    ((SettableConfigItem) factory.l2DSOConfig().host()).setValue(l2Host);
    return manager;
  }
}
