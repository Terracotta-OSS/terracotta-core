/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.groups.StripeIDStateManager;
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
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.storage.api.OffheapStats;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCumulativeCounter;

import java.util.Collections;

import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

public class DSOMBeanRegTest {

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
    
    when(channel.getRemoteAddress()).thenReturn(new TCSocketAddress(8192));
    when(channel.createMessage(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE)).thenReturn(msg);
    when(chMgr.getChannel(cidCollector.capture())).thenReturn(channel);
    when(msg.getChannel()).thenReturn(channel);

    channelMgrMbean = new DSOChannelManagerImpl(new GroupID(), chMgr,
                                                mock(TCConnectionManager.class), "1.0",
                                                mock(StripeIDStateManager.class));
    Mockito.doAnswer(new Answer() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        lsnrCaptor.getValue().channelRemoved(channel);
        return null;
      }

    }).when(channel).close();

    when(smCtxt.getChannelManager()).thenReturn((DSOChannelManagerMBean) channelMgrMbean);
    when(stats.getCounter(Matchers.same(channel), Matchers.startsWith("serverMap")))
        .thenReturn(mock(SampledCumulativeCounter.class));
    when(stats.getCounter(Matchers.same(channel), Matchers.argThat(new ArgumentMatcher<String>() {

      @Override
      public boolean matches(Object argument) {
        // Ugly, ugly, ugly
        String str = (String) argument;
        return !str.startsWith("serverMap");
      }

    }))).thenReturn(SampledCounter.NULL_SAMPLED_COUNTER);
    when(smCtxt.getChannelStats()).thenReturn(stats);
    mbeanSvr = mock(MBeanServer.class);
    dso = new DSO(smCtxt, mock(ServerConfigurationContext.class), mbeanSvr,
                      new GCStatsEventPublisher(),
                  mock(TerracottaOperatorEventHistoryProvider.class), mock(OffheapStats.class));

  }

  @Test
  public void testRemoveListenerOnDisconnect() throws Exception {
    channelMgrMbean.makeChannelActive(clientId, false);
    channelMgrMbean.closeAll(Collections.singletonList(clientId));
    InOrder order = Mockito.inOrder(mbeanSvr);

    order.verify(mbeanSvr).addNotificationListener(isA(ObjectName.class),
                                                     Matchers.argThat(new ArgumentMatcher<NotificationListener>() {

                                                       @Override
                                                       public boolean matches(Object arg) {
                                                         return arg instanceof DSOClient
                                                                && clientId.toLong() == (((DSOClient) arg)
                                                                                                .getClientID());
                                                       }

                                                   }), isA(NotificationFilter.class), Matchers.any());
    order.verify(mbeanSvr).removeNotificationListener(isA(ObjectName.class),
                                                        Matchers.argThat(new ArgumentMatcher<NotificationListener>() {

                                                          @Override
                                                          public boolean matches(Object arg) {
                                                            return arg instanceof DSOClient
                                                                   && clientId.toLong() == (((DSOClient) arg)
                                                                       .getClientID());
                                                          }

                                                      }), isA(NotificationFilter.class), Matchers.any());
  }

}
