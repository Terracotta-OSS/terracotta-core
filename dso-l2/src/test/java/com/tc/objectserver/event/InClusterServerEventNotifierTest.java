package com.tc.objectserver.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.ClientID;
import com.tc.object.net.DSOChannelManager;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.EnumSet;

/**
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifierTest {

  private InClusterServerEventNotifier notifier;
  private ServerEventBatcher batcher;

  @Before
  public void setUp() {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    batcher = mock(ServerEventBatcher.class);
    notifier = new InClusterServerEventNotifier(channelManager, batcher);
  }

  @Test
  public void testShouldCorrectlyRegister() throws Exception {
    final ClientID clientId1 = new ClientID(1L);
    final ClientID clientId2 = new ClientID(2L);
    final ClientID clientId3 = new ClientID(3L);

    notifier.register(clientId1, "cache1", EnumSet.of(ServerEventType.PUT, ServerEventType.EVICT));
    notifier.register(clientId1, "cache2", EnumSet.of(ServerEventType.PUT));
    notifier.register(clientId2, "cache1", EnumSet.of(ServerEventType.EXPIRE));
    notifier.register(clientId2, "cache1", EnumSet.of(ServerEventType.EVICT));
    notifier.register(clientId2, "cache3", EnumSet.of(ServerEventType.EXPIRE, ServerEventType.EVICT));
    notifier.register(clientId3, "cache2", EnumSet.of(ServerEventType.PUT, ServerEventType.EXPIRE));

    final ServerEvent putEvent = new BasicServerEvent(ServerEventType.PUT, 1001, new byte[] { 101 }, "cache1");
    notifier.handleServerEvent(putEvent);
    final ServerEvent expireEvent = new BasicServerEvent(ServerEventType.EXPIRE, 1002, "cache3");
    notifier.handleServerEvent(expireEvent);
    final ServerEvent evictEvent = new BasicServerEvent(ServerEventType.EVICT, 1003, "cache1");
    notifier.handleServerEvent(evictEvent);

    verify(batcher).add(clientId1, putEvent);
    verify(batcher).add(clientId1, evictEvent);
    verify(batcher).add(clientId2, evictEvent);
    verify(batcher).add(clientId2, expireEvent);
    verifyNoMoreInteractions(batcher);
  }

  @Test
  public void testShouldCorrectlyUnregister() {
    // TODO
  }

}
