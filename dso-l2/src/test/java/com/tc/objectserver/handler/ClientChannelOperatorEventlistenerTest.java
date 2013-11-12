package com.tc.objectserver.handler;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.tc.license.ProductID;
import com.tc.net.ClientID;
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

  private MessageChannel messageChannelWithProductID(ProductID productID) {
    MessageChannel messageChannel = mock(MessageChannel.class);
    when(messageChannel.getProductId()).thenReturn(productID);
    when(messageChannel.getRemoteNodeID()).thenReturn(new ClientID(1));
    return messageChannel;
  }
}
