package com.tc.object.handler;

import org.junit.Test;

import com.tc.async.api.Sink;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.context.ServerEventDeliveryContext;
import com.tc.object.msg.ServerEventBatchMessage;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventMessageHandlerTest {

  @Test
  public void testMustQueueUpEventsToDeliveryStage() {
    final ServerEvent event1 = new BasicServerEvent(ServerEventType.PUT, "key1", 0L, "test-cache");
    final ServerEvent event2 = new BasicServerEvent(ServerEventType.REMOVE, "key1", 1L, "test-cache");
    final ServerEvent event3 = new BasicServerEvent(ServerEventType.EVICT, "key2", "test-cache");
    final List<ServerEvent> events = Arrays.asList(event1, event2, event3);
    final NodeID remoteNode = new GroupID(0);

    final Sink sink = mock(Sink.class);
    final MessageChannel channel = mock(MessageChannel.class);
    final ServerEventBatchMessage msg = mock(ServerEventBatchMessage.class);
    when(msg.getChannel()).thenReturn(channel);
    when(channel.getRemoteNodeID()).thenReturn(remoteNode);
    when(msg.getEvents()).thenReturn(events);

    final ServerEventMessageHandler handler = new ServerEventMessageHandler(sink);
    handler.handleEvent(msg);

    verify(sink, times(3)).add(any(ServerEventDeliveryContext.class));
  }
}
