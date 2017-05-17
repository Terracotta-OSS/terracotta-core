/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import com.tc.cluster.ClusterImpl;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.L2ConfigForL1;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.TestThrowableHandler;
import com.tc.logging.TCLogging;
import com.tc.management.TCClient;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.core.SecurityInfo;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.object.config.ClientConfigImpl;
import com.tc.object.config.ConnectionInfoConfig;
import com.tc.object.config.PreparedComponentsFromL2Connection;
import com.tc.object.session.SessionProvider;
import com.tc.util.Assert;
import com.tc.util.PortChooser;
import com.tcclient.cluster.ClusterInternal;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.mockito.Matchers;
import org.mockito.Mockito;


public class DistributedObjectClientTest extends TestCase {
  
  public void testConnectionTimeout() throws Exception {
    L1ConfigurationSetupManager manager = new L1ConfigurationSetupManager() {
      @Override
      public String[] processArguments() {
        return new String[0];
      }

      @Override
      public boolean loadedFromTrustedSource() {
        return true;
      }

      @Override
      public String rawConfigText() {
        return "";
      }

      @Override
      public String source() {
        return "";
      }

      @Override
      public CommonL1Config commonL1Config() {
        return Mockito.mock(CommonL1Config.class);
      }

      @Override
      public L2ConfigForL1 l2Config() {
        return Mockito.mock(L2ConfigForL1.class);
      }

      @Override
      public SecurityInfo getSecurityInfo() {
        SecurityInfo si = Mockito.mock(SecurityInfo.class);
        Mockito.when(si.isSecure()).thenReturn(Boolean.FALSE);
        return si;
      }
    };
    PreparedComponentsFromL2Connection l2connection = Mockito.mock(PreparedComponentsFromL2Connection.class);
    ConnectionInfoConfig config = Mockito.mock(ConnectionInfoConfig.class);
    ConnectionInfo info = new ConnectionInfo("localhost", new PortChooser().chooseRandomPort());
    Mockito.when(config.getConnectionInfos()).thenReturn(new ConnectionInfo[] {info});
    Mockito.when(l2connection.createConnectionInfoConfigItem()).thenReturn(config);
    ClusterInternal cluster = new ClusterImpl();
    TCThreadGroup threadGroup = new TCThreadGroup(new TestThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class)));
    DistributedObjectClient client = new DistributedObjectClient(new ClientConfigImpl(manager), threadGroup, l2connection, cluster);
    client.start();
    Assert.assertTrue(threadGroup.activeCount() > 0);
    long start = System.currentTimeMillis();
    boolean connected = client.waitForConnection(10, TimeUnit.SECONDS);
    Assert.assertFalse(connected);
    client.shutdown();
    Assert.assertTrue(threadGroup.activeCount() == 0);
    Assert.assertTrue(System.currentTimeMillis() - start < 11000);
    int count = Thread.activeCount();
    System.out.println("active threads:" + count);
    Thread[] t = new Thread[count];
    Thread.enumerate(t);
    for (Thread z : t) {
      if (z != null ) {
        System.out.println(z.getName());
      }
    }
  }
  
  public void testFatalError() throws Exception {
    L1ConfigurationSetupManager manager = new L1ConfigurationSetupManager() {
      @Override
      public String[] processArguments() {
        return new String[0];
      }

      @Override
      public boolean loadedFromTrustedSource() {
        return true;
      }

      @Override
      public String rawConfigText() {
        return "";
      }

      @Override
      public String source() {
        return "";
      }

      @Override
      public CommonL1Config commonL1Config() {
        return Mockito.mock(CommonL1Config.class);
      }

      @Override
      public L2ConfigForL1 l2Config() {
        return Mockito.mock(L2ConfigForL1.class);
      }

      @Override
      public SecurityInfo getSecurityInfo() {
        SecurityInfo si = Mockito.mock(SecurityInfo.class);
        Mockito.when(si.isSecure()).thenReturn(Boolean.FALSE);
        return si;
      }
    };
    PreparedComponentsFromL2Connection l2connection = Mockito.mock(PreparedComponentsFromL2Connection.class);
    ConnectionInfoConfig config = Mockito.mock(ConnectionInfoConfig.class);
    ConnectionInfo info = new ConnectionInfo("localhost", new PortChooser().chooseRandomPort());
    Mockito.when(config.getConnectionInfos()).thenReturn(new ConnectionInfo[] {info});
    Mockito.when(l2connection.createConnectionInfoConfigItem()).thenReturn(config);
    ClusterInternal cluster = new ClusterImpl();
    TCThreadGroup threadGroup = new TCThreadGroup(new TestThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class)));
    ClientBuilder builder = new StandardClientBuilder() {
      @Override
      public ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr, SessionProvider sessionProvider, int socketConnectTimeout, TCClient client) {
        ClientMessageChannel channel = Mockito.mock(ClientMessageChannel.class);
        try {
          Mockito.when(channel.open(Mockito.anyCollection(), Matchers.anyString(), Matchers.any(char[].class))).thenThrow(new RuntimeException("bad connection"));
        } catch (Exception exp) {
          
        }
        return channel;
      }
    };
    
    DistributedObjectClient client = new DistributedObjectClient(new ClientConfigImpl(manager), builder, threadGroup, l2connection, cluster, null, null, null, null, false);
    client.start();
    Assert.assertTrue(threadGroup.activeCount() > 0);
    long start = System.currentTimeMillis();
    try {
      client.waitForConnection(10, TimeUnit.SECONDS);
      Assert.fail();
    } catch (RuntimeException exp) {
//    expected
      Assert.assertNotNull(exp);
      Assert.assertEquals(exp.getCause().getMessage(), "bad connection");
    }
    client.shutdown();
    Assert.assertTrue(threadGroup.activeCount() == 0);
    Assert.assertTrue(System.currentTimeMillis() - start < 11000);
    int count = Thread.activeCount();
    System.out.println("active threads:" + count);
    Thread[] t = new Thread[count];
    Thread.enumerate(t);
    for (Thread z : t) {
      if (z != null ) {
        System.out.println(z.getName());
      }
    }
  }  
}
