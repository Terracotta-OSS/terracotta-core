package com.tc.objectserver.impl;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.ClientID;
import com.tc.object.InterestType;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.interest.EvictionInterest;
import com.tc.objectserver.interest.ExpirationInterest;
import com.tc.objectserver.interest.Interest;
import com.tc.objectserver.interest.PutInterest;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Eugene Shelestovich
 */
public class InClusterInterestNotifierTest {

  private MockInterestNotifier notifier;

  @Before
  public void setUp() {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    notifier = new MockInterestNotifier(channelManager);
  }

  @Test
  public void testShouldCorrectlyRegister() throws Exception {
    notifier.register(new ClientID(1L), "cache1", EnumSet.of(InterestType.PUT, InterestType.EVICT));
    notifier.register(new ClientID(1L), "cache2", EnumSet.of(InterestType.PUT));
    notifier.register(new ClientID(2L), "cache1", EnumSet.of(InterestType.EXPIRE));
    notifier.register(new ClientID(2L), "cache1", EnumSet.of(InterestType.EVICT));
    notifier.register(new ClientID(2L), "cache3", EnumSet.of(InterestType.EXPIRE, InterestType.EVICT));
    notifier.register(new ClientID(3L), "cache2", EnumSet.of(InterestType.PUT, InterestType.EXPIRE));

    notifier.onInterest(new PutInterest(1001, new byte[] { 101 }, "cache1"));
    assertEquals(1, notifier.messages.size());

    Message msg = notifier.messages.get(0);
    assertEquals(new ClientID(1L), msg.clientId);
    assertEquals(InterestType.PUT, msg.type);
    assertEquals(1001, msg.key);
    assertEquals("cache1", msg.destination);

    notifier.onInterest(new ExpirationInterest(1002, "cache3"));
    assertEquals(2, notifier.messages.size());

    msg = notifier.messages.get(1);
    assertEquals(new ClientID(2L), msg.clientId);
    assertEquals(InterestType.EXPIRE, msg.type);
    assertEquals(1002, msg.key);
    assertEquals("cache3", msg.destination);

    notifier.onInterest(new EvictionInterest(1003, "cache1"));
    assertEquals(4, notifier.messages.size());

    msg = notifier.messages.get(2);
    assertEquals(new ClientID(1L), msg.clientId);
    assertEquals(InterestType.EVICT, msg.type);
    assertEquals(1003, msg.key);
    assertEquals("cache1", msg.destination);

    msg = notifier.messages.get(3);
    assertEquals(new ClientID(2L), msg.clientId);
    assertEquals(InterestType.EVICT, msg.type);
    assertEquals(1003, msg.key);
    assertEquals("cache1", msg.destination);
  }

  @Test
  public void testShouldCorrectlyUnregister() {

  }

  private static final class MockInterestNotifier extends InClusterInterestNotifier {

    private final List<Message> messages = new ArrayList<Message>();

    public MockInterestNotifier(final DSOChannelManager channelManager) {
      super(channelManager);
    }

    @Override
    void sendNotification(final ClientID clientId, final Interest interest) {
      messages.add(new Message(clientId, interest.getKey(), interest.getType(), interest.getCacheName()));
    }
  }

  private static final class Message {
    private final ClientID clientId;
    private final Object key;
    private final InterestType type;
    private final String destination;

    private Message(final ClientID clientId, final Object key, final InterestType type, final String destination) {
      this.clientId = clientId;
      this.key = key;
      this.type = type;
      this.destination = destination;
    }
  }

}
