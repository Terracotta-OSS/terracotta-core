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
package com.tc.objectserver.handler;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.tc.license.ProductID;
import com.tc.management.RemoteManagement;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.ClientID;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.operatorevent.NodeNameProvider;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author tim
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ TerracottaOperatorEventLogging.class})
public class ClientChannelOperatorEventlistenerTest {
  private TerracottaOperatorEventLogger logger;
  private ClientChannelOperatorEventlistener listener;

  @Before
  public void setUp() throws Exception {
    logger = spy(new TerracottaOperatorEventLogger(NodeNameProvider.DEFAULT_NODE_NAME_PROVIDER));
    PowerMockito.mockStatic(TerracottaOperatorEventLogging.class);
    when(TerracottaOperatorEventLogging.getEventLogger()).thenReturn(logger);
    listener = new ClientChannelOperatorEventlistener();
    RemoteManagement remoteManagement = mock(RemoteManagement.class);
    TerracottaRemoteManagement.setRemoteManagementInstance(remoteManagement);
  }

  @After
  public void tearDown() {
    TerracottaRemoteManagement.setRemoteManagementInstance(null);
  }

  @Test
  public void testNoOperatorEventForInternalClientJoin() throws Exception {
    listener.channelCreated(messageChannelWithProductID(ProductID.WAN));
    verify(logger, never()).fireOperatorEvent(any(TerracottaOperatorEvent.class));
  }

  @Test
  public void testNoOperatorEventForInternalClientLeave() throws Exception {
    listener.channelRemoved(messageChannelWithProductID(ProductID.TMS));
    verify(logger, never()).fireOperatorEvent(any(TerracottaOperatorEvent.class));
  }

  @Test
  public void testFireJoinedOperatorEventForNormalClient() throws Exception {
    listener.channelCreated(messageChannelWithProductID(ProductID.USER));
    verify(logger).fireOperatorEvent(any(TerracottaOperatorEvent.class));
  }

  @Test
  public void testFireNodeLeftOperatorEventForNormalClient() throws Exception {
    listener.channelRemoved(messageChannelWithProductID(ProductID.USER));
    verify(logger).fireOperatorEvent(any(TerracottaOperatorEvent.class));
  }

  @Test
  public void testNoOperatorEventForReconnectWindowClose() throws Exception {
    MessageChannel channel = messageChannelWithProductID(ProductID.USER);
    when(channel.getRemoteAddress()).thenReturn(null);
    listener.channelRemoved(channel);
    verify(logger, never()).fireOperatorEvent(any(TerracottaOperatorEvent.class));
  }

  private MessageChannel messageChannelWithProductID(ProductID productID) {
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(messageChannel.getProductId()).thenReturn(productID);
    when(messageChannel.getRemoteNodeID()).thenReturn(new ClientID(1));
    when(messageChannel.getRemoteAddress()).thenReturn(new TCSocketAddress(0));
    return messageChannel;
  }
}
