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

import com.tc.lang.TCThreadGroup;
import com.tc.lang.TestThrowableHandler;
import com.tc.net.ClientID;
import com.tc.net.core.ClearTextSocketEndpointFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.util.Assert;
import com.tc.net.core.ProductID;
import com.tc.net.protocol.tcm.ChannelID;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.utilities.test.net.PortManager;


public class DistributedObjectClientTest extends TestCase {

  public void testConnectionTimeout() throws Exception {
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      TCThreadGroup threadGroup = new TCThreadGroup(new TestThrowableHandler(LoggerFactory.getLogger(DistributedObjectClient.class)));
      Properties connectionProperties = new Properties();
      connectionProperties.put(ConnectionPropertyNames.CONNECTION_TYPE, ProductID.PERMANENT);
      DistributedObjectClient client =
          new DistributedObjectClient(
              Collections.singleton(new InetSocketAddress("localhost", portRef.port())),
              threadGroup,
              connectionProperties
          );
      long start = System.currentTimeMillis();
      boolean connected = client.connectFor(10, TimeUnit.SECONDS);
      Assert.assertFalse(connected);
      client.shutdown();
      Assert.assertTrue(threadGroup.activeCount() == 0);
      Assert.assertTrue(System.currentTimeMillis() - start, System.currentTimeMillis() - start < 15000);
    }
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
    try (PortManager.PortRef portRef = PortManager.getInstance().reservePort()) {
      TCThreadGroup threadGroup = new TCThreadGroup(new TestThrowableHandler(LoggerFactory.getLogger(DistributedObjectClient.class)));
      Properties connectionProperties = new Properties();
      connectionProperties.put(ConnectionPropertyNames.CONNECTION_TYPE, ProductID.PERMANENT);
      ClientBuilder builder = new StandardClientBuilder(connectionProperties, new ClearTextSocketEndpointFactory()) {
        @Override
        public ClientMessageChannel createClientMessageChannel(CommunicationsManager commMgr, int socketConnectTimeout) {
          ClientMessageChannel channel = Mockito.mock(ClientMessageChannel.class);
          try {
            Mockito.when(channel.open(Mockito.anyCollection())).thenThrow(new RuntimeException("bad connection"));
          } catch (Exception exp) {

          }
          when(channel.getProductID()).thenReturn(ProductID.PERMANENT);
          when(channel.getClientID()).thenReturn(new ClientID(1));
          when(channel.getChannelID()).thenReturn(new ChannelID(1));
          return channel;
        }
      };

      DistributedObjectClient client = new DistributedObjectClient(
          Collections.singleton(InetSocketAddress.createUnresolved("localhost", portRef.port())),
          builder,
          threadGroup,
          null,
          null);
      long start = System.currentTimeMillis();
      try {
        client.connectFor(10, TimeUnit.SECONDS);
        Assert.fail();
      } catch (RuntimeException exp) {
  //    expected
        Assert.assertNotNull(exp);
        Assert.assertEquals(exp.getCause().getMessage(), "bad connection");
      }
      client.shutdown();
      Assert.assertTrue(threadGroup.activeCount() == 0);
      Assert.assertTrue(System.currentTimeMillis() - start < 11000);
    }
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
