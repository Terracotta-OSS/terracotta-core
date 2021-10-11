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
package com.tc.stats;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelManager;
import com.tc.net.protocol.tcm.ChannelManagerEventListener;
import com.tc.net.protocol.tcm.MessageChannelInternal;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClientHandshakeAckMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;
import java.net.InetSocketAddress;

import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.junit.Ignore;

public class MBeanRegTest {

  private DSOChannelManager channelMgrMbean;

  DSO                       dso;
  private MBeanServer       mbeanSvr;
  private final ClientID    clientId = new ClientID(0);

  @Before
  public void setUp() throws Exception {
    // Mocks
    ServerManagementContext smCtxt = Mockito.mock(ServerManagementContext.class, Mockito.RETURNS_MOCKS);
    ClientHandshakeAckMessage msg = mock(ClientHandshakeAckMessage.class);
    final MessageChannelInternal channel = mock(MessageChannelInternal.class);
    ChannelManager chMgr = mock(ChannelManager.class);
    ChannelStats stats = mock(ChannelStats.class);

    final ArgumentCaptor<ChannelID> cidCollector = ArgumentCaptor.forClass(ChannelID.class);

    when(channel.getChannelID()).then(new Answer<ChannelID>() {

      @Override
      public ChannelID answer(InvocationOnMock invocation) throws Throwable {
        return cidCollector.getValue();
      }

    });
    
    final ArgumentCaptor<ChannelManagerEventListener> lsnrCaptor = ArgumentCaptor.forClass(ChannelManagerEventListener.class);
    Mockito.doNothing().when(chMgr).addEventListener(lsnrCaptor.capture());
    
    when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress(8192));
    when(channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE)).thenReturn(msg);
    when(chMgr.getChannel(cidCollector.capture())).thenReturn(channel);
    when(msg.getChannel()).thenReturn(channel);

    channelMgrMbean = new DSOChannelManagerImpl(chMgr, "1.0");
    Mockito.doAnswer(new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        lsnrCaptor.getValue().channelRemoved(channel);
        return null;
      }

    }).when(channel).close();

    when(smCtxt.getChannelManager()).thenReturn((DSOChannelManagerMBean) channelMgrMbean);
    when(stats.getCounter(ArgumentMatchers.same(channel), ArgumentMatchers.startsWith("serverMap")))
        .thenReturn(mock(SampledCumulativeCounter.class));
    when(stats.getCounter(ArgumentMatchers.same(channel), ArgumentMatchers.argThat(new ArgumentMatcher<String>() {

      @Override
      public boolean matches(String argument) {
        // Ugly, ugly, ugly
        String str = argument;
        return !str.startsWith("serverMap");
      }

    }))).thenReturn(SampledCounter.NULL_SAMPLED_COUNTER);
    when(smCtxt.getChannelStats()).thenReturn(stats);
    mbeanSvr = mock(MBeanServer.class);
    dso = new DSO(smCtxt, mock(ServerConfigurationContext.class), mbeanSvr);

  }

  @Test @Ignore // client tunneling is no longer valid
  public void testRemoveListenerOnDisconnect() throws Exception {
    channelMgrMbean.makeChannelActive(clientId);
    channelMgrMbean.closeAll(Collections.singletonList(clientId));
    InOrder order = Mockito.inOrder(mbeanSvr);

    order.verify(mbeanSvr).addNotificationListener(isA(ObjectName.class),
                                                     ArgumentMatchers.argThat(new ArgumentMatcher<NotificationListener>() {

                                                       @Override
                                                       public boolean matches(NotificationListener arg) {
                                                         return arg instanceof Client
                                                                && clientId.toLong() == (((Client) arg)
                                                                                                .getClientID());
                                                       }

                                                   }), isA(NotificationFilter.class), ArgumentMatchers.any());
    order.verify(mbeanSvr).removeNotificationListener(isA(ObjectName.class),
                                                        ArgumentMatchers.argThat(new ArgumentMatcher<NotificationListener>() {

                                                          @Override
                                                          public boolean matches(NotificationListener arg) {
                                                            return arg instanceof Client
                                                                   && clientId.toLong() == (((Client) arg)
                                                                       .getClientID());
                                                          }

                                                      }), isA(NotificationFilter.class), ArgumentMatchers.any());
  }

}
