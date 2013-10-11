/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.connect;

import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage;
import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage.Connecting;
import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage.Disconnecting;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnection;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import junit.framework.Assert;

public class ClientConnectEventHandlerTest {

  private ConcurrentMap<ChannelID, JMXConnector>               channelIdToJmxConnector;
  private ConcurrentMap<ChannelID, TunnelingMessageConnection> channelIdToMsgConnection;
  @Mock
  private MessageChannel                                       channel;
  @Mock
  private MBeanServer                                          mbs;
  @Mock
  private JMXConnector                                         jmxConnector;
  @Mock
  private ClientBeanBag                                        clientBeanBag;

  private final class ClientConnectEventHandlerforTest extends ClientConnectEventHandler {
    @Override
    protected JMXConnector getJmxConnector(JMXServiceURL serviceURL, Map environment) {
      return jmxConnector;
    }

    @Override
    protected ClientBeanBag createClientBeanBag(final L1ConnectionMessage msg, final MessageChannel channel1,
                                                final MBeanServer l2MBeanServer,
                                                final MBeanServerConnection l1MBeanServerConnection) {
      return clientBeanBag;
    }
    int getClientBeanBagsMapSize() {
      return clientBeanBags.size();
    }
  }

  private final ClientConnectEventHandlerforTest clientConnectEventHandler = new ClientConnectEventHandlerforTest();

  @Before
  public void setUp() throws Throwable {
    MockitoAnnotations.initMocks(this);
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress(59899));
    when(channel.getChannelID()).thenReturn(new ChannelID(1234567890L));
    when(jmxConnector.getMBeanServerConnection()).thenReturn(null);
    when(clientBeanBag.updateRegisteredBeans()).thenReturn(true);
    Mockito
        .doNothing()
        .when(jmxConnector)
        .addConnectionNotificationListener((NotificationListener) Matchers.any(), (NotificationFilter) Matchers.any(),
                                           Matchers.any());
    channelIdToJmxConnector = new ConcurrentHashMap<ChannelID, JMXConnector>();
    channelIdToMsgConnection = new ConcurrentHashMap<ChannelID, TunnelingMessageConnection>();
  }

  @Test
  public void testEventHandleWithDisconnecting() throws Throwable {

    Connecting context = new Connecting(mbs, channel, null, null, channelIdToJmxConnector, channelIdToMsgConnection);
    clientConnectEventHandler.handleEvent(context);
    Assert.assertEquals(1, clientConnectEventHandler.getClientBeanBagsMapSize());
    Disconnecting context2 = new Disconnecting(channel, channelIdToJmxConnector, channelIdToMsgConnection);
    clientConnectEventHandler.handleEvent(context2);
    Assert.assertEquals(0, clientConnectEventHandler.getClientBeanBagsMapSize());
  }

}
