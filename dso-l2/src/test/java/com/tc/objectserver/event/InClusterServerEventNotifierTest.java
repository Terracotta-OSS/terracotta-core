package com.tc.objectserver.event;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.ClientID;
import com.tc.object.net.DSOChannelManager;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;

import java.util.EnumSet;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.EXPIRE;
import static com.tc.server.ServerEventType.PUT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifierTest {

  private final ClientID clientId1 = new ClientID(1L);
  private final ClientID clientId2 = new ClientID(2L);
  private final ClientID clientId3 = new ClientID(3L);

  private InClusterServerEventNotifier notifier;
  private ServerEventBatcher batcher;

  @Before
  public void setUp() {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    batcher = mock(ServerEventBatcher.class);
    notifier = new InClusterServerEventNotifier(channelManager, batcher);

    notifier.register(clientId1, "cache1", EnumSet.of(PUT, EVICT));
    notifier.register(clientId1, "cache2", EnumSet.of(PUT));
    notifier.register(clientId2, "cache1", EnumSet.of(EXPIRE));
    notifier.register(clientId2, "cache1", EnumSet.of(EVICT));
    notifier.register(clientId2, "cache3", EnumSet.of(EXPIRE, EVICT));
    notifier.register(clientId3, "cache2", EnumSet.of(PUT, EXPIRE));
  }

  @Test
  public void testShouldCorrectlyRouteEventsAfterRegistration() throws Exception {
    final ServerEvent event1 = new BasicServerEvent(PUT, 1001, "cache1");
    final ServerEvent event2 = new BasicServerEvent(EXPIRE, 1002, "cache3");
    final ServerEvent event3 = new BasicServerEvent(EVICT, 1003, "cache1");
    final ServerEvent event4 = new BasicServerEvent(PUT, 1004, "cache3");

    notifier.handleServerEvent(event1);
    notifier.handleServerEvent(event2);
    notifier.handleServerEvent(event3);
    notifier.handleServerEvent(event4);

    verify(batcher).add(clientId1, event1);
    verify(batcher).add(clientId2, event2);
    verify(batcher).add(clientId1, event3);
    verify(batcher).add(clientId2, event3);
    verify(batcher, never()).add(any(ClientID.class), eq(event4));
    verifyNoMoreInteractions(batcher);
  }

  @Test
  public void testShouldCorrectlyRouteEventsAfterUnregisteration() {
    final ServerEvent event1 = new BasicServerEvent(EVICT, 1001, "cache1");
    final ServerEvent event2 = new BasicServerEvent(EXPIRE, 1002, "cache1");
    final ServerEvent event3 = new BasicServerEvent(PUT, 1003, "cache2");

    notifier.unregister(clientId2, "cache1", EnumSet.of(EVICT, PUT));
    notifier.unregister(clientId3, "cache2", EnumSet.of(PUT, EXPIRE));

    notifier.handleServerEvent(event1);
    notifier.handleServerEvent(event2);
    notifier.handleServerEvent(event3);

    verify(batcher).add(clientId1, event1);
    verify(batcher, never()).add(clientId2, event1);
    verify(batcher).add(clientId2, event2);
    verify(batcher).add(clientId1, event3);
    verify(batcher, never()).add(clientId3, event3);
    verifyNoMoreInteractions(batcher);
  }

}
