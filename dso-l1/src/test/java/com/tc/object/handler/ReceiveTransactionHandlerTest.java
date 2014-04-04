package com.tc.object.handler;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.context.ServerEventDeliveryContext;
import com.tc.object.msg.BroadcastTransactionMessage;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

/**
 * @author Eugene Shelestovich
 */
public class ReceiveTransactionHandlerTest {

  @Test
  public void testMustSendEventsToDeliveryStage() {
    final Sink deliverySink = mock(Sink.class);

    final ReceiveTransactionHandler handler = new ReceiveTransactionHandler(null, null, null, deliverySink);

    final ServerEvent event1 = new BasicServerEvent(ServerEventType.PUT, "k1", "cache-1");
    final ServerEvent event2 = new BasicServerEvent(ServerEventType.REMOVE, "k1", "cache-1");
    final ServerEvent event3 = new BasicServerEvent(ServerEventType.PUT, "k2", "cache-2");

    final NodeID remoteNodeId = new ClientID(1L);

    final MessageChannel channel = mock(MessageChannel.class);
    when(channel.getRemoteNodeID()).thenReturn(remoteNodeId);

    final BroadcastTransactionMessage transactionMsg = mock(BroadcastTransactionMessage.class);
    when(transactionMsg.getChannel()).thenReturn(channel);
    when(transactionMsg.getEvents()).thenReturn(Lists.newArrayList(event1, event2, event3));

    handler.sendServerEvents(transactionMsg);

    verify(deliverySink).add(eq(new ServerEventDeliveryContext(event1, remoteNodeId)));
    verify(deliverySink).add(eq(new ServerEventDeliveryContext(event2, remoteNodeId)));
    verify(deliverySink).add(eq(new ServerEventDeliveryContext(event3, remoteNodeId)));
  }
}
