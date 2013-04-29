package com.tc.objectserver.impl;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.ClientID;
import com.tc.object.ServerEventType;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.event.BasicServerEvent;
import com.tc.objectserver.event.ServerEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifierTest {

  private MockServerEventNotifier notifier;

  @Before
  public void setUp() {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    notifier = new MockServerEventNotifier(channelManager);
  }

  @Test
  public void testShouldCorrectlyRegister() throws Exception {
    notifier.register(new ClientID(1L), "cache1", EnumSet.of(ServerEventType.PUT, ServerEventType.EVICT));
    notifier.register(new ClientID(1L), "cache2", EnumSet.of(ServerEventType.PUT));
    notifier.register(new ClientID(2L), "cache1", EnumSet.of(ServerEventType.EXPIRE));
    notifier.register(new ClientID(2L), "cache1", EnumSet.of(ServerEventType.EVICT));
    notifier.register(new ClientID(2L), "cache3", EnumSet.of(ServerEventType.EXPIRE, ServerEventType.EVICT));
    notifier.register(new ClientID(3L), "cache2", EnumSet.of(ServerEventType.PUT, ServerEventType.EXPIRE));

    notifier.handleServerEvent(new BasicServerEvent(ServerEventType.PUT, 1001, new byte[] { 101 }, "cache1"));
    assertEquals(1, notifier.messages.size());

    Message msg = notifier.messages.get(0);
    assertEquals(new ClientID(1L), msg.clientId);
    assertEquals(ServerEventType.PUT, msg.type);
    assertEquals(1001, msg.key);
    assertEquals("cache1", msg.destination);

    notifier.handleServerEvent(new BasicServerEvent(ServerEventType.EXPIRE, 1002, "cache3"));
    assertEquals(2, notifier.messages.size());

    msg = notifier.messages.get(1);
    assertEquals(new ClientID(2L), msg.clientId);
    assertEquals(ServerEventType.EXPIRE, msg.type);
    assertEquals(1002, msg.key);
    assertEquals("cache3", msg.destination);

    notifier.handleServerEvent(new BasicServerEvent(ServerEventType.EVICT, 1003, "cache1"));
    assertEquals(4, notifier.messages.size());

    msg = notifier.messages.get(2);
    assertEquals(new ClientID(1L), msg.clientId);
    assertEquals(ServerEventType.EVICT, msg.type);
    assertEquals(1003, msg.key);
    assertEquals("cache1", msg.destination);

    msg = notifier.messages.get(3);
    assertEquals(new ClientID(2L), msg.clientId);
    assertEquals(ServerEventType.EVICT, msg.type);
    assertEquals(1003, msg.key);
    assertEquals("cache1", msg.destination);
  }

  @Test
  public void testShouldCorrectlyUnregister() {

  }

  private static final class MockServerEventNotifier extends InClusterServerEventNotifier {

    private final List<Message> messages = new ArrayList<Message>();

    public MockServerEventNotifier(final DSOChannelManager channelManager) {
      super(channelManager);
    }

    @Override
    void sendNotification(final ClientID clientId, final ServerEvent event) {
      messages.add(new Message(clientId, event.getKey(), event.getType(), event.getCacheName()));
    }
  }

  private static final class Message {
    private final ClientID clientId;
    private final Object key;
    private final ServerEventType type;
    private final String destination;

    private Message(final ClientID clientId, final Object key, final ServerEventType type, final String destination) {
      this.clientId = clientId;
      this.key = key;
      this.type = type;
      this.destination = destination;
    }
  }

}
